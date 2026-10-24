package com.gyote.silvercare.medical_document.infrastructure;

import com.gyote.silvercare.medical_document.domain.DocumentStorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 로컬 파일시스템 원본 저장소의 저장·삭제·경로 탈출 차단·서명 URL 형식을 확인하는 테스트입니다. */
class LocalDocumentStorageTest {

    private static final String KEY = "documents/a/b.pdf";
    private static final byte[] CONTENT = {1, 2, 3};

    @TempDir
    Path root;

    private LocalDocumentStorage storage;

    @BeforeEach
    void setUp() {
        storage = new LocalDocumentStorage(root.toString());
    }

    @Test
    void storeOriginalWritesFileUnderRoot() {
        storeSample();

        assertThat(root.resolve(KEY)).exists().hasBinaryContent(CONTENT);
    }

    @Test
    void deleteOriginalRemovesFile() {
        storeSample();

        storage.deleteOriginal(KEY);

        assertThat(root.resolve(KEY)).doesNotExist();
    }

    @Test
    void deleteOriginalIgnoresMissingFile() {
        assertThatCode(() -> storage.deleteOriginal("missing.pdf")).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"../escape.pdf", "a/../../escape.pdf", "", "."})
    void rejectsKeyNotInsideRoot(String storageKey) {
        assertThatThrownBy(() -> storage.storeOriginal(storageKey, CONTENT, "application/pdf"))
                .isInstanceOf(DocumentStorageException.class);
        assertThatThrownBy(() -> storage.deleteOriginal(storageKey))
                .isInstanceOf(DocumentStorageException.class);
    }

    @Test
    void createSignedUrlKeepsLocalFormat() {
        String url = storage.createSignedUrl(KEY, Duration.ofMinutes(5));

        assertThat(url).startsWith("http://localhost:8080/local-documents/" + KEY + "?expiresAt=");
    }

    private void storeSample() {
        storage.storeOriginal(KEY, CONTENT, "application/pdf");
    }
}
