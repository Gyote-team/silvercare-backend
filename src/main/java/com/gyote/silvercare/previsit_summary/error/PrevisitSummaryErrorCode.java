package com.gyote.silvercare.previsit_summary.error;

import com.gyote.silvercare.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

/** 진료 전 요약 요청·결과 조회 오류. */
public enum PrevisitSummaryErrorCode implements ErrorCode {
    NOT_FOUND(HttpStatus.NOT_FOUND,"PREVISIT_001","요약 요청 또는 결과를 찾을 수 없습니다."),
    UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE,"PREVISIT_002","진료 전 요약을 지금 이용할 수 없습니다."),
    TOO_MANY_RECORDS(HttpStatus.BAD_REQUEST,"PREVISIT_003","기록이 많습니다. 기간을 줄여 다시 요청해 주세요."),
    SOURCE_CHANGED(HttpStatus.CONFLICT,"PREVISIT_004","원본 기록이 변경됐습니다. 요약을 새로 만들어 주세요."),
    NOT_READY(HttpStatus.CONFLICT,"PREVISIT_005","아직 요약이 완료되지 않았습니다.");
    private final HttpStatus status; private final String code; private final String message;
    PrevisitSummaryErrorCode(HttpStatus status,String code,String message) {
        this.status=status; this.code=code; this.message=message;
    }
    public HttpStatus status(){return status;}
    public String code(){return code;}
    public String message(){return message;}
}
