package com.gyote.silvercare.ai_result.domain.repository;

import com.gyote.silvercare.ai_result.domain.entity.AiLlmUsageEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface AiLlmUsageEventRepository extends JpaRepository<AiLlmUsageEvent, UUID> { }
