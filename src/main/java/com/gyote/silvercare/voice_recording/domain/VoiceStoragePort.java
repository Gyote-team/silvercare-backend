package com.gyote.silvercare.voice_recording.domain;

/** 음성 원본 저장소를 교체할 수 있는 포트. */
public interface VoiceStoragePort {
    /** 서버 생성 키로 원본을 저장한다. */
    void store(String key, byte[] bytes);

    /** 롤백한 업로드의 원본을 정리한다. */
    void remove(String key);
}
