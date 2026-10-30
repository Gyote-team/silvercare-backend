package com.gyote.silvercare.voice_recording.infrastructure;

import com.gyote.silvercare.global.exception.BusinessException;
import com.gyote.silvercare.voice_recording.domain.VoiceStoragePort;
import com.gyote.silvercare.voice_recording.error.VoiceRecordingErrorCode;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;

/** 외부에 공개하지 않는 공유 Object Storage의 음성 영역. */
@Component
public class VoiceStorage implements VoiceStoragePort {
    private final Path root;

    public VoiceStorage(
            @Value("${silvercare.storage.local-root:./data/object-storage}") String root) {
        this.root =
                Path.of(root == null || root.isBlank() ? "./data/object-storage" : root)
                        .toAbsolutePath()
                        .normalize();
    }

    /** 서버가 생성한 키에 원본을 저장한다. */
    public void store(String key, byte[] bytes) {
        try {
            Path path = resolve(key);
            Files.createDirectories(path.getParent());
            Files.write(path, bytes, StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            throw new BusinessException(VoiceRecordingErrorCode.UNAVAILABLE);
        }
    }

    /** DB 롤백으로 남은 원본만 정리한다. */
    public void remove(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException e) {
            throw new BusinessException(VoiceRecordingErrorCode.UNAVAILABLE);
        }
    }

    private Path resolve(String key) {
        Path path = root.resolve(key).normalize();
        if (!path.startsWith(root) || !key.startsWith("voice/"))
            throw new IllegalArgumentException("Invalid voice storage key");
        return path;
    }
}
