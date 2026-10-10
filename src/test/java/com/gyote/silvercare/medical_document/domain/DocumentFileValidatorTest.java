package com.gyote.silvercare.medical_document.domain;

import com.gyote.silvercare.global.exception.BusinessException;
import com.gyote.silvercare.medical_document.error.MedicalDocumentErrorCode;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 업로드 파일 검증기가 내용 기준 형식·크기·페이지 수·해상도·PDF 비밀번호 규칙대로 통과시키거나 거절하는지 확인하는 테스트입니다. */
class DocumentFileValidatorTest {

    private static final long MAX_BYTES = 200_000;
    private static final int MAX_PDF_PAGES = 2;
    private static final long MAX_IMAGE_PIXELS = 10_000;
    private static final String NO_USER_PASSWORD = "";

    private final DocumentFileValidator validator =
            new DocumentFileValidator(MAX_BYTES, MAX_PDF_PAGES, MAX_IMAGE_PIXELS);

    @Test
    void acceptsJpeg() {
        assertThat(validator.validate(image("jpg", 10, 10))).isEqualTo(DocumentFileType.JPEG);
    }

    @Test
    void acceptsPng() {
        assertThat(validator.validate(image("png", 10, 10))).isEqualTo(DocumentFileType.PNG);
    }

    @Test
    void acceptsPdf() {
        assertThat(validator.validate(pdf(1))).isEqualTo(DocumentFileType.PDF);
    }

    @Test
    void acceptsFileAtByteLimit() {
        byte[] content = pdf(1);
        DocumentFileValidator exactLimit =
                new DocumentFileValidator(content.length, MAX_PDF_PAGES, MAX_IMAGE_PIXELS);

        assertThat(exactLimit.validate(content)).isEqualTo(DocumentFileType.PDF);
    }

    @Test
    void acceptsImageAtPixelLimit() {
        assertThat(validator.validate(image("png", 100, 100))).isEqualTo(DocumentFileType.PNG);
    }

    @Test
    void acceptsPdfAtPageLimit() {
        assertThat(validator.validate(pdf(MAX_PDF_PAGES))).isEqualTo(DocumentFileType.PDF);
    }

    @Test
    void acceptsPermissionRestrictedPdfWithoutUserPassword() {
        assertThat(validator.validate(protectedPdf(1, NO_USER_PASSWORD))).isEqualTo(DocumentFileType.PDF);
    }

    @Test
    void rejectsEmptyFile() {
        assertRejected(new byte[0], MedicalDocumentErrorCode.EMPTY_FILE);
        assertRejected(null, MedicalDocumentErrorCode.EMPTY_FILE);
    }

    @Test
    void rejectsTooLargeFile() {
        assertRejected(new byte[(int) MAX_BYTES + 1], MedicalDocumentErrorCode.FILE_TOO_LARGE);
    }

    @Test
    void rejectsBytesWithoutSignature() {
        assertRejected(new byte[]{1, 2, 3, 4, 5, 6, 7, 8}, MedicalDocumentErrorCode.UNSUPPORTED_FILE_TYPE);
    }

    @Test
    void rejectsPngWithSignatureOnly() {
        byte[] signatureOnly = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

        assertRejected(signatureOnly, MedicalDocumentErrorCode.UNSUPPORTED_FILE_TYPE);
    }

    @Test
    void rejectsImageOverPixelLimit() {
        assertRejected(image("png", 200, 200), MedicalDocumentErrorCode.IMAGE_RESOLUTION_EXCEEDED);
    }

    @Test
    void rejectsPdfOverPageLimit() {
        assertRejected(pdf(MAX_PDF_PAGES + 1), MedicalDocumentErrorCode.PDF_PAGE_LIMIT_EXCEEDED);
    }

    @Test
    void rejectsBrokenPdf() {
        byte[] broken = "%PDF-1.7\nthis is not a pdf body".getBytes(StandardCharsets.US_ASCII);

        assertRejected(broken, MedicalDocumentErrorCode.UNSUPPORTED_FILE_TYPE);
    }

    @Test
    void rejectsPdfWithUserPassword() {
        assertRejected(protectedPdf(1, "user-password"), MedicalDocumentErrorCode.ENCRYPTED_PDF);
    }

    @Test
    void rejectsPermissionRestrictedPdfOverPageLimit() {
        assertRejected(protectedPdf(MAX_PDF_PAGES + 1, NO_USER_PASSWORD),
                MedicalDocumentErrorCode.PDF_PAGE_LIMIT_EXCEEDED);
    }

    private void assertRejected(byte[] content, MedicalDocumentErrorCode errorCode) {
        assertThatThrownBy(() -> validator.validate(content))
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
        return pdf(pages, null);
    }

    /** 소유자 비밀번호로 인쇄·내용 복사를 막은 PDF를 만듭니다. userPassword가 비어 있으면 비밀번호 없이 열리는 권한 제한 PDF입니다. */
    private static byte[] protectedPdf(int pages, String userPassword) {
        AccessPermission permission = new AccessPermission();
        permission.setCanPrint(false);
        permission.setCanExtractContent(false);
        return pdf(pages, new StandardProtectionPolicy("owner-password", userPassword, permission));
    }

    private static byte[] pdf(int pages, StandardProtectionPolicy policy) {
        try (PDDocument document = new PDDocument()) {
            for (int i = 0; i < pages; i++) {
                document.addPage(new PDPage());
            }
            if (policy != null) {
                document.protect(policy);
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            document.save(output);
            return output.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
