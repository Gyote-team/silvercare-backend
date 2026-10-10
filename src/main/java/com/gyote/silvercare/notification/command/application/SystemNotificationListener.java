package com.gyote.silvercare.notification.command.application;

import com.gyote.silvercare.notification.domain.SystemActivityEvent;
import com.gyote.silvercare.notification.domain.SystemActivityType;
import com.gyote.silvercare.notification.domain.entity.SystemNotification;
import com.gyote.silvercare.notification.domain.repository.SystemNotificationRepository;
import com.gyote.silvercare.patient.domain.repository.PatientRepository;
import com.gyote.silvercare.user.domain.User;
import com.gyote.silvercare.user.domain.repository.UserRepository;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** 도메인 활동과 같은 트랜잭션에서 당사자의 시스템 알림을 생성한다. */
@Component
public class SystemNotificationListener {
    private final PatientRepository patients;
    private final UserRepository users;
    private final SystemNotificationRepository notifications;

    public SystemNotificationListener(
            PatientRepository patients,
            UserRepository users,
            SystemNotificationRepository notifications) {
        this.patients = patients;
        this.users = users;
        this.notifications = notifications;
    }

    /** 기록 본문을 알림에 복제하지 않고 당사자에게 시스템 활동을 알린다. */
    @EventListener
    public void onActivity(SystemActivityEvent event) {
        var patient = patients.findById(event.patientId()).orElseThrow();
        var recipient =
                switch (event.type()) {
                    case RELATION_ACCEPTED, RELATION_REJECTED -> event.caregiverId();
                    case RELATION_REVOKED ->
                            patient.getUserId().equals(event.actorId())
                                    ? event.caregiverId()
                                    : patient.getUserId();
                    case RELATION_REQUESTED, RELATION_CANCELED, HEALTH_RECORD_CREATED ->
                            patient.getUserId();
                };
        if (recipient.equals(event.actorId())) {
            return;
        }
        String actor = users.findById(event.actorId()).map(User::getName).orElse("이용자");
        String message =
                actor
                        + " 님"
                        + switch (event.type()) {
                            case RELATION_REQUESTED -> "이 보호자 연결을 요청했어요.";
                            case RELATION_ACCEPTED -> "이 연결 요청을 수락했어요.";
                            case RELATION_REJECTED -> "이 연결 요청을 거절했어요.";
                            case RELATION_CANCELED -> "이 연결 요청을 취소했어요.";
                            case RELATION_REVOKED -> "이 보호자 연결을 해제했어요.";
                            case HEALTH_RECORD_CREATED -> "이 내 건강기록을 작성했어요.";
                        };
        notifications.save(
                new SystemNotification(
                        recipient,
                        event.type(),
                        message,
                        event.type() != SystemActivityType.HEALTH_RECORD_CREATED
                                ? "/home"
                                : "/records"));
    }
}
