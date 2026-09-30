package com.gyote.silvercare.medical_document.infrastructure;

import com.gyote.silvercare.medical_document.domain.DocumentStoragePort;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * 로컬 개발용 임시 저장소 구현입니다. 실제 파일에 접근하지 않고 가짜 서명 URL만 만듭니다.
 * 이슈 #18에서 S3 구현으로 교체합니다.
 */
@Component
public class LocalDocumentStorage implements DocumentStoragePort {

    private static final String BASE_URL = "http://localhost:8080/local-documents/";

    @Override
    public String createSignedUrl(String storageKey, Duration ttl) {
        return BASE_URL + storageKey + "?expiresAt=" + Instant.now().plus(ttl);
    }
}
