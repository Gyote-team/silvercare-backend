package com.gyote.silvercare.care_relation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;
import java.time.Instant;

/** 보호자 열람 동의의 조회용 엔티티입니다. */
@Entity
@Table(name = "consents")
public class Consent {

    @Id
    private UUID id;

    @Column(name = "caregiver_link_id", nullable = false)
    private UUID caregiverLinkId;

    @Column(nullable = false)
    private String scope;

    @Column(nullable = false)
    private String status;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;
}
