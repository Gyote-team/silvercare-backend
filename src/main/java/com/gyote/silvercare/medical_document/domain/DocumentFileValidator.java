package com.gyote.silvercare.medical_document.domain;

import com.gyote.silvercare.global.exception.BusinessException;
import com.gyote.silvercare.medical_document.error.MedicalDocumentErrorCode;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;

/**
 * 업로드 파일이 JPEG·PNG·PDF인지 내용 시그니처로만 판정하고, 크기·PDF 페이지 수·이미지 해상도 한도를 확인하는 코드입니다.
 * 요청의 Content-Type은 판정에 쓰지 않으며, 열람 비밀번호가 필요한 PDF는 ENCRYPTED_PDF로 거절합니다.
 */
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

    /** 업로드 파일을 정해진 순서로 검사하고, 통과하면 내용 시그니처로 판별한 실제 파일 형식을 반환합니다. 요청의 Content-Type은 보지 않습니다. */
    public DocumentFileType validate(byte[] content) {
        checkSize(content);
        DocumentFileType actualType = DocumentFileType.findBySignature(content)
                .orElseThrow(DocumentFileValidator::unsupportedFileType);
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

    /** 비밀번호 없이 열리는 PDF(소유자 비밀번호만 걸린 권한 제한 PDF 포함)의 쪽수를 반환합니다. 열람 비밀번호가 필요하면 ENCRYPTED_PDF로 거절합니다. */
    private int readPdfPageCount(byte[] content) {
        try (PDDocument document = Loader.loadPDF(content)) {
            return document.getNumberOfPages();
        } catch (InvalidPasswordException e) {
            throw new BusinessException(MedicalDocumentErrorCode.ENCRYPTED_PDF);
        } catch (IOException | RuntimeException e) {
            throw unsupportedFileType();
        }
    }

    private static BusinessException unsupportedFileType() {
        return new BusinessException(MedicalDocumentErrorCode.UNSUPPORTED_FILE_TYPE);
    }
}
