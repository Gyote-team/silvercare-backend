package com.gyote.silvercare.medical_document.domain;

import java.time.Duration;

/** 의료 문서 원본이 저장된 Object Storage 접근 포트입니다. */
public interface DocumentStoragePort {

    /** storageKey 원본을 ttl 동안만 열 수 있는 서명 URL을 발급합니다. */
    String createSignedUrl(String storageKey, Duration ttl);
}
