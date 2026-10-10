package com.gyote.silvercare.health_record.query.application;
import com.gyote.silvercare.health_record.domain.HealthRecordAccessPolicy;
import com.gyote.silvercare.health_record.domain.entity.HealthRecord;
import com.gyote.silvercare.health_record.domain.repository.HealthRecordRepository;
import com.gyote.silvercare.health_record.error.HealthRecordErrorCode;
import com.gyote.silvercare.health_record.query.model.*;
import com.gyote.silvercare.global.exception.BusinessException;
import com.gyote.silvercare.user.domain.User;
import com.gyote.silvercare.user.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@Transactional(readOnly=true)
public class HealthRecordQueryService {
    private final HealthRecordRepository records;
    private final HealthRecordAccessPolicy access;
    private final UserRepository users;
    public HealthRecordQueryService(HealthRecordRepository records, HealthRecordAccessPolicy access, UserRepository users) {
        this.records=records; this.access=access; this.users=users;
    }
    /** 최신순 커서 목록을 반환하며 삭제된 기록은 제외한다. */
    public HealthRecordPageView list(User actor, UUID patientId, String cursor, Integer requestedSize) {
        UUID target = access.requirePatient(actor, patientId);
        int size = requestedSize==null ? 20 : requestedSize;
        if (size<1 || size>100) throw new BusinessException(HealthRecordErrorCode.INVALID_REQUEST);
        Instant before=null;
        UUID beforeId=null;
        if (cursor!=null) {
            try {
                String[] parts = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8).split("\\|", -1);
                if (parts.length!=3 || !target.equals(UUID.fromString(parts[0]))) throw new IllegalArgumentException();
                before=Instant.parse(parts[1]); beforeId=UUID.fromString(parts[2]);
            } catch (RuntimeException error) { throw new BusinessException(HealthRecordErrorCode.INVALID_REQUEST); }
        }
        List<HealthRecord> rows = before == null
                ? records.findFirstPage(target, PageRequest.of(0,size+1))
                : records.findPage(target, before, beforeId, PageRequest.of(0,size+1));
        boolean hasNext = rows.size()>size;
        List<HealthRecord> shown=rows.subList(0, Math.min(size, rows.size()));
        Map<UUID,String> names=new HashMap<>();
        users.findAllById(shown.stream().map(HealthRecord::getAuthorUserId).distinct().toList())
             .forEach(user -> names.put(user.getId(),user.getName()));
        String next=null;
        if (hasNext) {
            HealthRecord last=shown.get(shown.size()-1);
            next=Base64.getUrlEncoder().withoutPadding().encodeToString(
                (target+"|"+last.getCreatedAt()+"|"+last.getId()).getBytes(StandardCharsets.UTF_8));
        }
        return new HealthRecordPageView(shown.stream().map(r -> view(r,names.getOrDefault(r.getAuthorUserId(),"이용자"))).toList(), next, hasNext);
    }
    /** 상세 기록의 대상 개인 접근 권한을 검증한다. */
    public HealthRecordView detail(User actor, UUID id) {
        HealthRecord record=records.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new BusinessException(HealthRecordErrorCode.NOT_FOUND));
        access.requirePatient(actor, record.getPatientId());
        return view(record, users.findById(record.getAuthorUserId()).map(User::getName).orElse("이용자"));
    }
    /** 저장된 기록을 조회 모델로 변환한다. */
    public static HealthRecordView view(HealthRecord r, String authorName) {
        return new HealthRecordView(r.getId(),r.getPatientId(),r.getAuthorUserId(),authorName,r.getVisitId(),
                r.getContent(),r.getRecordedAt(),r.getCreatedAt(),r.getUpdatedAt(),r.isProxyWritten());
    }
}
