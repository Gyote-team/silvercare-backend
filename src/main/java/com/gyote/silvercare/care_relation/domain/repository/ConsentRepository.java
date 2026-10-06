package com.gyote.silvercare.care_relation.domain.repository;

import com.gyote.silvercare.care_relation.domain.Consent;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

/** 활성 돌봄 관계의 동의 범위를 조회합니다. */
public interface ConsentRepository extends Repository<Consent, UUID> {

    @Query(value = """
            SELECT CASE WHEN COUNT(DISTINCT c.scope) = 2 THEN TRUE ELSE FALSE END
            FROM consents c
            WHERE c.caregiver_link_id = :careRelationId
              AND c.status = 'ACTIVE'
              AND c.revoked_at IS NULL
              AND (c.expires_at IS NULL OR c.expires_at > :now)
              AND c.scope IN ('DOCUMENT', 'SUMMARY')
            """, nativeQuery = true)
    boolean hasDocumentAndSummaryConsent(
            @Param("careRelationId") UUID careRelationId,
            @Param("now") Instant now
    );
}
