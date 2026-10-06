package com.gyote.silvercare.medical_document.domain;

/** 원본 파일 저장소에 쓰거나 지우는 작업이 실패했을 때 던지는 예외입니다. 업로드 저장 실패는 DocumentUploadCommandService가 STORAGE_UNAVAILABLE(503)로 바꿉니다. */
public class DocumentStorageException extends RuntimeException {

    /** 실패 이유만 담은 저장소 예외를 만듭니다. */
    public DocumentStorageException(String message) {
        super(message);
    }

    /** 실패 이유와 원인 예외를 담은 저장소 예외를 만듭니다. */
    public DocumentStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
