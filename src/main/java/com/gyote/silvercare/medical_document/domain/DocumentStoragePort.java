package com.gyote.silvercare.medical_document.domain;

import java.time.Duration;

/** 의료 문서 원본이 저장된 Object Storage 접근 포트입니다. */
public interface DocumentStoragePort {

    /** storageKey 원본을 ttl 동안만 열 수 있는 서명 URL을 발급합니다. */
    String createSignedUrl(String storageKey, Duration ttl);

    /** 원본 바이트를 storageKey 위치에 저장합니다. mimeType은 저장소의 Content-Type으로 쓰며, 실패하면 DocumentStorageException을 던집니다. */
    void storeOriginal(String storageKey, byte[] content, String mimeType);

    /** storageKey 원본을 삭제합니다. 파일이 없으면 아무 일도 하지 않고, 실패하면 DocumentStorageException을 던집니다. */
    void deleteOriginal(String storageKey);
}
