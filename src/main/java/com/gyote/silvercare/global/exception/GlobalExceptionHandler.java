package com.gyote.silvercare.global.exception;

import com.gyote.silvercare.medical_document.error.MedicalDocumentErrorCode;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        return ResponseEntity.status(errorCode.status()).body(ErrorResponse.of(errorCode));
    }

    /** 경로·쿼리·본문의 입력 오류를 같은 공통 코드로 반환한다. */
    @ExceptionHandler({
        MethodArgumentNotValidException.class,
        HttpMessageNotReadableException.class,
        MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ErrorResponse> handleInvalidRequest(Exception exception) {
        return ResponseEntity.badRequest().body(ErrorResponse.of(GlobalErrorCode.INVALID_REQUEST));
    }

    /** 동시에 발생한 수정·삭제 충돌을 공통 409 오류로 반환한다. */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleConcurrentUpdate(
            ObjectOptimisticLockingFailureException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(GlobalErrorCode.CONFLICT));
    }

    /** multipart 크기 한도를 넘은 요청을 413 FILE_TOO_LARGE 응답으로 바꿔 반환합니다. */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleFileTooLarge(
            MaxUploadSizeExceededException exception) {
        ErrorCode errorCode = MedicalDocumentErrorCode.FILE_TOO_LARGE;
        return ResponseEntity.status(errorCode.status()).body(ErrorResponse.of(errorCode));
    }

    /** 파일 파트가 없거나 multipart 형식이 깨진 요청을 400 INVALID_REQUEST 응답으로 바꿔 반환합니다. */
    @ExceptionHandler({MissingServletRequestPartException.class, MultipartException.class})
    public ResponseEntity<ErrorResponse> handleInvalidMultipart(Exception exception) {
        return ResponseEntity.badRequest().body(ErrorResponse.of(GlobalErrorCode.INVALID_REQUEST));
    }
}
