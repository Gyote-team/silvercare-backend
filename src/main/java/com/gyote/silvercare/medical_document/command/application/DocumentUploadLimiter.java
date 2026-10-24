package com.gyote.silvercare.medical_document.command.application;

import com.gyote.silvercare.global.exception.BusinessException;
import com.gyote.silvercare.medical_document.error.MedicalDocumentErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.concurrent.Semaphore;
import java.util.function.Supplier;

/** 동시에 처리하는 업로드 수를 제한하는 코드입니다. */
@Component
public class DocumentUploadLimiter {

    private final Semaphore permits;

    public DocumentUploadLimiter(@Value("${silvercare.document.upload.max-concurrent:4}") int maxConcurrent) {
        Assert.isTrue(maxConcurrent >= 1, "silvercare.document.upload.max-concurrent는 1 이상이어야 합니다.");
        this.permits = new Semaphore(maxConcurrent);
    }

    /** 자리가 있으면 task를 실행하고, 없으면 기다리지 않고 TOO_MANY_UPLOADS를 던집니다. */
    public <T> T run(Supplier<T> task) {
        if (!permits.tryAcquire()) {
            throw new BusinessException(MedicalDocumentErrorCode.TOO_MANY_UPLOADS);
        }
        try {
            return task.get();
        } finally {
            permits.release();
        }
    }
}
