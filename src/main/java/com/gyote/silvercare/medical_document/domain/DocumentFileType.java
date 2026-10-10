package com.gyote.silvercare.medical_document.domain;

import java.util.Arrays;
import java.util.Optional;

/** 업로드를 허용하는 원본 파일 형식(JPEG·PNG·PDF)입니다. 각 형식은 MIME 타입·확장자·파일 시그니처를 가집니다. */
public enum DocumentFileType {
    JPEG("image/jpeg", "jpg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}),
    PNG("image/png", "png", new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}),
    PDF("application/pdf", "pdf", new byte[]{0x25, 0x50, 0x44, 0x46, 0x2D});

    private final String mimeType;
    private final String extension;
    private final byte[] signature;

    DocumentFileType(String mimeType, String extension, byte[] signature) {
        this.mimeType = mimeType;
        this.extension = extension;
        this.signature = signature;
    }

    /** 이 형식의 MIME 타입을 반환합니다. */
    public String getMimeType() {
        return mimeType;
    }

    /** 이 형식의 대표 확장자를 반환합니다. */
    public String getExtension() {
        return extension;
    }

    /** 파일 앞부분 시그니처로 실제 형식을 찾아 반환합니다. 판별할 수 없으면 빈 Optional을 반환합니다. */
    public static Optional<DocumentFileType> findBySignature(byte[] content) {
        return Arrays.stream(values())
                .filter(type -> type.startsWithSignature(content))
                .findFirst();
    }

    private boolean startsWithSignature(byte[] content) {
        return content.length >= signature.length
                && Arrays.equals(content, 0, signature.length, signature, 0, signature.length);
    }
}
