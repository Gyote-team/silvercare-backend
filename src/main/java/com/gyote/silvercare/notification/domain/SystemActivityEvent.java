package com.gyote.silvercare.notification.domain;
import java.util.UUID;
/** 사용자 행동과 같은 트랜잭션에서 생성할 시스템 알림 이벤트. */
public record SystemActivityEvent(String type, UUID patientId, UUID caregiverId, UUID actorId, UUID subjectId) {}
