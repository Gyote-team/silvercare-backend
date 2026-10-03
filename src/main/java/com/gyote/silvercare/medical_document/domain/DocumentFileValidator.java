package com.gyote.silvercare.medical_document.domain;

import com.gyote.silvercare.global.exception.BusinessException;
import com.gyote.silvercare.medical_document.error.MedicalDocumentErrorCode;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;

/** 업로드 파일이 JPEG·PNG·PDF인지 실제 내용으로 검사하고, 크기·PDF 페이지 수·이미지 해상도 한도를 확인하는 코드입니다. */
@Component
public class DocumentFileValidator {

    private final long maxBytes;
    private final int maxPdfPages;
    private final long maxImagePixels;

    public DocumentFileValidator(
            @Value("${silvercare.document.upload.max-bytes:20971520}") long maxBytes,
            @Value("${silvercare.document.upload.max-pdf-pages:30}") int maxPdfPages,
            @Value("${silvercare.document.upload.max-image-pixels:40000000}") long maxImagePixels
    ) {
        this.maxBytes = maxBytes;
        this.maxPdfPages = maxPdfPages;
        this.maxImagePixels = maxImagePixels;
    }

    /** 업로드 파일을 정해진 순서로 검사하고, 통과하면 내용으로 판별한 실제 파일 형식을 반환합니다. */
    public DocumentFileType validate(byte[] content, String contentType) {
        checkSize(content);
        DocumentFileType declaredType = findDeclaredType(contentType);
        DocumentFileType actualType = findActualType(content, declaredType);
        if (actualType == DocumentFileType.PDF) {
            checkPdfPages(content);
        } else {
            checkImagePixels(content);
        }
        return actualType;
    }

    private void checkSize(byte[] content) {
        if (content == null || content.length == 0) {
            throw new BusinessException(MedicalDocumentErrorCode.EMPTY_FILE);
        }
        if (content.length > maxBytes) {
            throw new BusinessException(MedicalDocumentErrorCode.FILE_TOO_LARGE);
        }
    }

    private DocumentFileType findDeclaredType(String contentType) {
        if (contentType == null) {
            throw unsupportedFileType();
        }
        String mimeType = contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        return DocumentFileType.findByMimeType(mimeType)
                .orElseThrow(DocumentFileValidator::unsupportedFileType);
    }

    private DocumentFileType findActualType(byte[] content, DocumentFileType declaredType) {
        return DocumentFileType.findBySignature(content)
                .filter(actualType -> actualType == declaredType)
                .orElseThrow(DocumentFileValidator::unsupportedFileType);
    }

    private void checkImagePixels(byte[] content) {
        if (readPixelCount(content) > maxImagePixels) {
            throw new BusinessException(MedicalDocumentErrorCode.IMAGE_RESOLUTION_EXCEEDED);
        }
    }

    private long readPixelCount(byte[] content) {
        try (ImageInputStream input = new MemoryCacheImageInputStream(new ByteArrayInputStream(content))) {
            ImageReader reader = findImageReader(input);
            try {
                reader.setInput(input, true, true);
                return (long) reader.getWidth(0) * reader.getHeight(0);
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException e) {
            throw unsupportedFileType();
        }
    }

    private ImageReader findImageReader(ImageInputStream input) {
        Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
        if (!readers.hasNext()) {
            throw unsupportedFileType();
        }
        return readers.next();
    }

    private void checkPdfPages(byte[] content) {
        if (readPdfPageCount(content) > maxPdfPages) {
            throw new BusinessException(MedicalDocumentErrorCode.PDF_PAGE_LIMIT_EXCEEDED);
        }
    }

    private int readPdfPageCount(byte[] content) {
        try (PDDocument document = Loader.loadPDF(content)) {
            if (document.isEncrypted()) {
                throw unsupportedFileType();
            }
            return document.getNumberOfPages();
        } catch (IOException | RuntimeException e) {
            throw unsupportedFileType();
        }
    }

    private static BusinessException unsupportedFileType() {
        return new BusinessException(MedicalDocumentErrorCode.UNSUPPORTED_FILE_TYPE);
    }
}
