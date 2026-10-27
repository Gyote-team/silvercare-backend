package com.gyote.silvercare.voice_recording.command.application;

import com.gyote.silvercare.global.exception.*;
import com.gyote.silvercare.global.status.TranscriptionStatus;
import com.gyote.silvercare.health_record.command.application.HealthRecordCommandService;
import com.gyote.silvercare.user.domain.User;
import com.gyote.silvercare.voice_recording.domain.VoiceAccessPolicy;
import com.gyote.silvercare.voice_recording.domain.VoiceStoragePort;
import com.gyote.silvercare.voice_recording.domain.entity.VoiceTranscription;
import com.gyote.silvercare.voice_recording.domain.repository.VoiceTranscriptionRepository;
import com.gyote.silvercare.voice_recording.error.VoiceRecordingErrorCode;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** STT 결과가 도착해도 사용자 확정 전에는 건강기록을 생성하지 않는다. */
@Service
@Transactional
public class VoiceCommandService {
    private final VoiceTranscriptionRepository jobs;
    private final VoiceAccessPolicy access;
    private final VoiceStoragePort storage;
    private final HealthRecordCommandService records;
    private final EntityManager em;
    private final boolean enabled;

    public VoiceCommandService(
            VoiceTranscriptionRepository jobs,
            VoiceAccessPolicy access,
            VoiceStoragePort storage,
            HealthRecordCommandService records,
            EntityManager em,
            @Value("${silvercare.voice.enabled:false}") boolean enabled) {
        this.jobs = jobs;
        this.access = access;
        this.storage = storage;
        this.records = records;
        this.em = em;
        this.enabled = enabled;
    }

    /** 원본 검증과 중복 검사를 마친 업로드를 비동기 처리 대기열에 저장한다. */
    public UUID upload(
            User actor,
            UUID patientId,
            UUID visitId,
            boolean consultation,
            boolean consent,
            String key,
            byte[] bytes) {
        UUID target = access.patient(actor, patientId);
        if (!consent
                || key == null
                || !key.matches("[A-Za-z0-9_-]{8,100}")
                || (consultation && visitId == null))
            throw new BusinessException(GlobalErrorCode.INVALID_REQUEST);
        access.writableVisit(target, visitId);
        if (bytes.length > 20 * 1024 * 1024)
            throw new BusinessException(VoiceRecordingErrorCode.TOO_LARGE);
        String extension = format(bytes);
        String mime =
                switch (extension) {
                    case "wav" -> "audio/wav";
                    case "mp3" -> "audio/mpeg";
                    case "m4a" -> "audio/mp4";
                    case "ogg" -> "audio/ogg";
                    default -> "audio/webm";
                };
        String purpose = consultation ? "CONSULTATION" : "HEALTH_RECORD";
        String hash = hash(bytes);
        // 같은 사용자의 업로드를 직렬화하여 동시 재전송도 하나의 작업으로 처리한다.
        em.find(User.class, actor.getId(), LockModeType.PESSIMISTIC_WRITE);
        var existing = jobs.findByUploaderUserIdAndIdempotencyKey(actor.getId(), key);
        if (existing.isPresent()) {
            var row = existing.get();
            access.require(actor, row);
            if (!row.getPatientId().equals(target)
                    || !Objects.equals(row.getVisitId(), visitId)
                    || !row.getPurpose().equals(purpose)
                    || !row.getContentHash().equals(hash))
                throw new BusinessException(VoiceRecordingErrorCode.KEY_CONFLICT);
            return row.getId();
        }
        if (!enabled) throw new BusinessException(VoiceRecordingErrorCode.UNAVAILABLE);
        UUID id = UUID.randomUUID();
        String storageKey = "voice/" + target + "/" + id + "." + extension;
        storage.store(storageKey, bytes);
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status != STATUS_COMMITTED) storage.remove(storageKey);
                    }
                });
        jobs.saveAndFlush(
                VoiceTranscription.create(
                        id,
                        target,
                        actor.getId(),
                        visitId,
                        purpose,
                        key,
                        hash,
                        storageKey,
                        mime,
                        bytes.length));
        return id;
    }

    /** 최신 초안을 명시적으로 확인한 내용만 한 번 기록으로 확정한다. */
    public UUID confirm(User actor, UUID id, String body, Long version) {
        var row = locked(actor, id);
        if (body == null
                || body.isBlank()
                || body.length() > 2000
                || version == null
                || version < 0) throw new BusinessException(GlobalErrorCode.INVALID_REQUEST);
        if (row.getStatus() == TranscriptionStatus.CONFIRMED) {
            if (!body.equals(row.getConfirmedText()))
                throw new BusinessException(VoiceRecordingErrorCode.INVALID_STATE);
            return row.getId();
        }
        if (row.getStatus() != TranscriptionStatus.DRAFT_READY)
            throw new BusinessException(VoiceRecordingErrorCode.INVALID_STATE);
        if (row.getVersion() != version) throw new BusinessException(GlobalErrorCode.CONFLICT);
        access.writableVisit(row.getPatientId(), row.getVisitId());
        var record = records.create(actor, row.getPatientId(), body, row.getVisitId());
        record.markSpeechInput();
        row.confirm(record.getId(), body);
        jobs.flush();
        return row.getId();
    }

    /** 초안 취소는 최종 기록을 만들지 않으며 재요청은 멱등이다. */
    public void cancel(User actor, UUID id) {
        var row = locked(actor, id);
        if (row.getStatus() == TranscriptionStatus.CONFIRMED)
            throw new BusinessException(VoiceRecordingErrorCode.INVALID_STATE);
        if (row.getStatus() != TranscriptionStatus.CANCELED) row.cancel();
    }

    /** 실패한 음성을 사용자 요청으로 재처리한다. */
    public UUID retry(User actor, UUID id) {
        var row = locked(actor, id);
        if (row.getStatus() != TranscriptionStatus.FAILED)
            throw new BusinessException(VoiceRecordingErrorCode.INVALID_STATE);
        access.writableVisit(row.getPatientId(), row.getVisitId());
        if (!enabled) throw new BusinessException(VoiceRecordingErrorCode.UNAVAILABLE);
        row.retry();
        jobs.flush();
        return id;
    }

    private VoiceTranscription locked(User actor, UUID id) {
        var row =
                jobs.findLockedById(id)
                        .orElseThrow(
                                () -> new BusinessException(VoiceRecordingErrorCode.NOT_FOUND));
        access.require(actor, row);
        return row;
    }

    private static String hash(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String ascii(byte[] bytes, int offset, int length) {
        return new String(bytes, offset, length, StandardCharsets.US_ASCII);
    }

    private static String format(byte[] b) {
        if (b.length < 16) throw new BusinessException(VoiceRecordingErrorCode.UNSUPPORTED);
        if (b.length >= 44 && ascii(b, 0, 4).equals("RIFF") && ascii(b, 8, 4).equals("WAVE"))
            return "wav";
        if ((b[0] & 255) == 0x1a
                && (b[1] & 255) == 0x45
                && (b[2] & 255) == 0xdf
                && (b[3] & 255) == 0xa3) return "webm";
        if (ascii(b, 0, 4).equals("OggS")) return "ogg";
        if (ascii(b, 0, 3).equals("ID3") || ((b[0] & 255) == 255 && (b[1] & 0xe0) == 0xe0))
            return "mp3";
        if (ascii(b, 4, 4).equals("ftyp")
                && (ascii(b, 8, 4).equals("M4A ")
                        || ascii(b, 8, 4).equals("isom")
                        || ascii(b, 8, 4).equals("mp42"))) return "m4a";
        throw new BusinessException(VoiceRecordingErrorCode.UNSUPPORTED);
    }
}
