package com.gyote.silvercare.medical_document.api.controller;

import com.gyote.silvercare.global.exception.BusinessException;
import com.gyote.silvercare.global.exception.GlobalErrorCode;
import com.gyote.silvercare.medical_document.api.dto.response.DocumentUploadResponseDto;
import com.gyote.silvercare.medical_document.api.mapper.MedicalDocumentResponseMapper;
import com.gyote.silvercare.medical_document.command.application.DocumentUploadCommand;
import com.gyote.silvercare.medical_document.command.application.DocumentUploadCommandService;
import com.gyote.silvercare.medical_document.command.application.DocumentUploadResult;
import com.gyote.silvercare.medical_document.command.application.MedicalDocumentCommandService;
import com.gyote.silvercare.medical_document.query.application.MedicalDocumentQueryService;
import com.gyote.silvercare.user.domain.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
public class MedicalDocumentCommandController {

    private final MedicalDocumentQueryService queries;
    private final MedicalDocumentCommandService commands;
    private final DocumentUploadCommandService uploads;
    private final MedicalDocumentResponseMapper responseMapper;

    public MedicalDocumentCommandController(
            MedicalDocumentQueryService queries,
            MedicalDocumentCommandService commands,
            DocumentUploadCommandService uploads,
            MedicalDocumentResponseMapper responseMapper
    ) {
        this.queries = queries;
        this.commands = commands;
        this.uploads = uploads;
        this.responseMapper = responseMapper;
    }

    /**
     * 방문에 의료 문서 원본(JPEG·PNG·PDF)을 업로드하는 API입니다. 새로 만들면 201, 같은 Idempotency-Key 재요청이면 200입니다.
     * 오류: 400(요청 값·키·문서 유형), 401(비로그인), 403(권한 없음), 404(방문 없음), 409(키 충돌), 413(크기 초과), 415(형식), 503(저장소 장애).
     */
    @PostMapping(value = "/api/visits/{visitId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentUploadResponseDto> upload(
            @AuthenticationPrincipal OAuth2User user,
            @PathVariable UUID visitId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String declaredDocType,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        User me = queries.requireUser(kakaoId(user));
        DocumentUploadResult result = uploads.upload(me, new DocumentUploadCommand(
                visitId, idempotencyKey, declaredDocType,
                file.getOriginalFilename(), file.getContentType(), readBytes(file)));
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(responseMapper.toUploadResponse(result));
    }

    @DeleteMapping("/api/documents/{documentId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal OAuth2User user,
            @PathVariable UUID documentId
    ) {
        User me = queries.requireUser(kakaoId(user));
        commands.delete(me, documentId);
        return ResponseEntity.noContent().build();
    }

    private static byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new BusinessException(GlobalErrorCode.INVALID_REQUEST);
        }
    }

    private static String kakaoId(OAuth2User user) {
        Object id = user.getAttributes().get("id");
        return id == null ? "" : String.valueOf(id);
    }
}
