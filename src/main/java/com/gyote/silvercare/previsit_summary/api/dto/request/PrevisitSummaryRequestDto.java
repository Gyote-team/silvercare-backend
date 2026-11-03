package com.gyote.silvercare.previsit_summary.api.dto.request;

import jakarta.validation.constraints.*;
import java.util.UUID;

/** 기록 ID는 클라이언트가 지정하지 않고 서버가 기간과 권한으로 선정한다. */
public record PrevisitSummaryRequestDto(UUID patientId, @Min(1) @Max(30) Integer windowDays) {}
