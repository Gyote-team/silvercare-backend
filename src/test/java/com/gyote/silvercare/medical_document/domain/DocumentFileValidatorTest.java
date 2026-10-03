package com.gyote.silvercare.medical_document.domain;

import com.gyote.silvercare.global.exception.BusinessException;
import com.gyote.silvercare.medical_document.error.MedicalDocumentErrorCode;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 업로드 파일 검증기가 형식·크기·페이지 수·해상도 규칙대로 통과시키거나 거절하는지 확인하는 테스트입니다. */
class DocumentFileValidatorTest {

    private static final long MAX_BYTES = 200_000;
    private static final int MAX_PDF_PAGES = 2;
    private static final long MAX_IMAGE_PIXELS = 10_000;

    private final DocumentFileValidator validator =
            new DocumentFileValidator(MAX_BYTES, MAX_PDF_PAGES, MAX_IMAGE_PIXELS);

    @Test
    void acceptsJpeg() {
        assertThat(validator.validate(image("jpg", 10, 10), "image/jpeg")).isEqualTo(DocumentFileType.JPEG);
    }

    @Test
    void acceptsPng() {
        assertThat(validator.validate(image("png", 10, 10), "image/png")).isEqualTo(DocumentFileType.PNG);
    }

    @Test
    void acceptsPdf() {
        assertThat(validator.validate(pdf(1), "application/pdf")).isEqualTo(DocumentFileType.PDF);
    }

    @Test
    void acceptsContentTypeWithParameters() {
        assertThat(validator.validate(pdf(1), "application/pdf; charset=UTF-8")).isEqualTo(DocumentFileType.PDF);
    }

    @Test
    void acceptsFileAtByteLimit() {
        byte[] content = pdf(1);
        DocumentFileValidator exactLimit =
                new DocumentFileValidator(content.length, MAX_PDF_PAGES, MAX_IMAGE_PIXELS);

        assertThat(exactLimit.validate(content, "application/pdf")).isEqualTo(DocumentFileType.PDF);
    }

    @Test
    void acceptsImageAtPixelLimit() {
        assertThat(validator.validate(image("png", 100, 100), "image/png")).isEqualTo(DocumentFileType.PNG);
    }

    @Test
    void acceptsPdfAtPageLimit() {
        assertThat(validator.validate(pdf(MAX_PDF_PAGES), "application/pdf")).isEqualTo(DocumentFileType.PDF);
    }

    @Test
    void rejectsNullContentType() {
        assertRejected(pdf(1), null, MedicalDocumentErrorCode.UNSUPPORTED_FILE_TYPE);
    }

    @Test
    void rejectsEmptyFile() {
        assertRejected(new byte[0], "application/pdf", MedicalDocumentErrorCode.EMPTY_FILE);
        assertRejected(null, "application/pdf", MedicalDocumentErrorCode.EMPTY_FILE);
    }

    @Test
    void rejectsTooLargeFile() {
        assertRejected(new byte[(int) MAX_BYTES + 1], "application/pdf", MedicalDocumentErrorCode.FILE_TOO_LARGE);
    }

    @Test
    void rejectsUnsupportedContentType() {
        assertRejected(pdf(1), "text/plain", MedicalDocumentErrorCode.UNSUPPORTED_FILE_TYPE);
    }

    @Test
    void rejectsPdfDisguisedAsPng() {
        assertRejected(pdf(1), "image/png", MedicalDocumentErrorCode.UNSUPPORTED_FILE_TYPE);
    }

    @Test
    void rejectsBytesWithoutSignature() {
        assertRejected(new byte[]{1, 2, 3, 4, 5, 6, 7, 8}, "image/jpeg", MedicalDocumentErrorCode.UNSUPPORTED_FILE_TYPE);
    }

    @Test
    void rejectsPngWithSignatureOnly() {
        byte[] signatureOnly = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

        assertRejected(signatureOnly, "image/png", MedicalDocumentErrorCode.UNSUPPORTED_FILE_TYPE);
    }

    @Test
    void rejectsImageOverPixelLimit() {
        assertRejected(image("png", 200, 200), "image/png", MedicalDocumentErrorCode.IMAGE_RESOLUTION_EXCEEDED);
    }

    @Test
    void rejectsPdfOverPageLimit() {
        assertRejected(pdf(MAX_PDF_PAGES + 1), "application/pdf", MedicalDocumentErrorCode.PDF_PAGE_LIMIT_EXCEEDED);
    }

    @Test
    void rejectsBrokenPdf() {
        byte[] broken = "%PDF-1.7\nthis is not a pdf body".getBytes(StandardCharsets.US_ASCII);

        assertRejected(broken, "application/pdf", MedicalDocumentErrorCode.UNSUPPORTED_FILE_TYPE);
    }

    private void assertRejected(byte[] content, String contentType, MedicalDocumentErrorCode errorCode) {
        assertThatThrownBy(() -> validator.validate(content, contentType))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(errorCode));
    }

    private static byte[] image(String format, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, format, output);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return output.toByteArray();
    }

    private static byte[] pdf(int pages) {
        try (PDDocument document = new PDDocument()) {
            for (int i = 0; i < pages; i++) {
                document.addPage(new PDPage());
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            document.save(output);
            return output.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}