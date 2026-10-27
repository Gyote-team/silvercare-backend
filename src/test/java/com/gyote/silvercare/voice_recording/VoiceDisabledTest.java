package com.gyote.silvercare.voice_recording;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.gyote.silvercare.global.exception.BusinessException;
import com.gyote.silvercare.health_record.command.application.HealthRecordCommandService;
import com.gyote.silvercare.user.domain.User;
import com.gyote.silvercare.voice_recording.command.application.VoiceCommandService;
import com.gyote.silvercare.voice_recording.domain.*;
import com.gyote.silvercare.voice_recording.domain.repository.VoiceTranscriptionRepository;
import com.gyote.silvercare.voice_recording.error.VoiceRecordingErrorCode;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;

import java.util.*;

class VoiceDisabledTest {
    @Test
    void disabledFeatureDoesNotStoreFileOrCreateQueueEntry() {
        var jobs = mock(VoiceTranscriptionRepository.class);
        var access = mock(VoiceAccessPolicy.class);
        var storage = mock(VoiceStoragePort.class);
        var records = mock(HealthRecordCommandService.class);
        var em = mock(EntityManager.class);
        var user = mock(User.class);
        UUID userId = UUID.randomUUID(), patientId = UUID.randomUUID();
        when(user.getId()).thenReturn(userId);
        when(access.patient(user, null)).thenReturn(patientId);
        when(jobs.findByUploaderUserIdAndIdempotencyKey(any(), any())).thenReturn(Optional.empty());
        byte[] wav = new byte[44];
        System.arraycopy("RIFF".getBytes(), 0, wav, 0, 4);
        System.arraycopy("WAVE".getBytes(), 0, wav, 8, 4);
        var service = new VoiceCommandService(jobs, access, storage, records, em, false);
        assertThatThrownBy(
                        () ->
                                service.upload(
                                        user,
                                        null,
                                        null,
                                        false,
                                        true,
                                        UUID.randomUUID().toString(),
                                        wav))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        error ->
                                org.assertj.core.api.Assertions.assertThat(error.getErrorCode())
                                        .isEqualTo(VoiceRecordingErrorCode.UNAVAILABLE));
        verifyNoInteractions(storage, records);
        verify(jobs, never()).saveAndFlush(any());
    }
}
