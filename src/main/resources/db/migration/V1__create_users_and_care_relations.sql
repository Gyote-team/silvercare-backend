-- 회원. 카카오 로그인. 역할은 한 번만 고른다.
CREATE TABLE users (
    id UUID NOT NULL,
    email VARCHAR(255),
    password_hash VARCHAR(255),
    name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    kakao_id VARCHAR(255),
    invite_code VARCHAR(16),
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    deleted_at TIMESTAMP(6) WITH TIME ZONE,
    PRIMARY KEY (id),
    CONSTRAINT users_role_check CHECK (role IN ('PENDING', 'PATIENT', 'CAREGIVER', 'ADMIN')),
    CONSTRAINT users_status_check CHECK (status IN ('ACTIVE', 'LOCKED', 'WITHDRAWN')),
    CONSTRAINT users_kakao_id_key UNIQUE (kakao_id),
    CONSTRAINT users_invite_code_key UNIQUE (invite_code)
);

-- 개인-보호자 연결. 코드 로그인이 아니라 연결 요청이다.
CREATE TABLE care_relations (
    id UUID NOT NULL,
    patient_id UUID NOT NULL,
    caregiver_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    requested_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    accepted_at TIMESTAMP(6) WITH TIME ZONE,
    ended_at TIMESTAMP(6) WITH TIME ZONE,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT care_relations_status_check CHECK (
        status IN ('REQUESTED', 'ACTIVE', 'REJECTED', 'CANCELED', 'REVOKED')
    ),
    CONSTRAINT care_relations_patient_fk FOREIGN KEY (patient_id) REFERENCES users (id),
    CONSTRAINT care_relations_caregiver_fk FOREIGN KEY (caregiver_id) REFERENCES users (id)
);

CREATE INDEX care_relations_patient_requested_idx
    ON care_relations (patient_id, requested_at);
CREATE INDEX care_relations_caregiver_requested_idx
    ON care_relations (caregiver_id, requested_at);
CREATE INDEX care_relations_pair_status_idx
    ON care_relations (patient_id, caregiver_id, status);
