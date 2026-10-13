package com.gyote.silvercare.visit.api.dto.request;

import com.gyote.silvercare.visit.domain.VisitStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

/** 방문 작성·수정 HTTP 입력. 개인은 patientId를 생략할 수 있다. */
public record VisitWriteRequestDto(
        UUID patientId,
        @NotNull LocalDate visitedOn,
        @Size(max = 200) String hospitalName,
        @Size(max = 100) String department,
        @Size(max = 2000) String memo,
        @NotNull VisitStatus visitStatus) {}
