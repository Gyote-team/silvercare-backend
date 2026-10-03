package com.gyote.silvercare.medical_document.domain.repository;

import com.gyote.silvercare.medical_document.domain.entity.DocumentAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** 문서 분석 작업(document_analyses)을 저장·조회하는 Repository입니다. */
public interface DocumentAnalysisRepository extends JpaRepository<DocumentAnalysis, UUID> {
}
