# 🗂️ SilverCare Backend 프로젝트 구조

이 문서는 **파일을 어디에 둘지** 판단하는 기준입니다. 계층별 상세 규칙은 [개발 규약](development-convention.md)을 따릅니다.

```text
backend/
├─ src/
│  ├─ main/
│  │  ├─ java/com/gyote/silvercare/
│  │  │  ├─ SilverCareApplication.java        # Spring Boot 실행 진입점
│  │  │  ├─ global/                           # 인증·설정·공통 예외·공통 웹 진입점
│  │  │  ├─ user/                             # 회원·역할
│  │  │  ├─ care_relation/                    # 개인-보호자 연결
│  │  │  ├─ health_record/                    # 건강기록 (구조 준비)
│  │  │  ├─ visit/                            # 병원 방문 (구조 준비)
│  │  │  ├─ medical_document/                 # 의료 문서·업로드 메타데이터 (구조 준비)
│  │  │  ├─ action_item/                      # 할 일 후보 승인·거절 (구조 준비)
│  │  │  ├─ schedule/                         # 일정·복약 알림 (구조 준비)
│  │  │  ├─ timeline/                         # 통합 타임라인 조회 (구조 준비)
│  │  │  └─ voice_recording/                  # 건강기록·진료실 녹음 메타데이터 (구조 준비)
│  │  └─ resources/
│  │     ├─ application.yml                   # Spring 환경 설정
│  │     ├─ db/migration/                     # Flyway DB 마이그레이션
│  │     └─ static/img/                       # 정적 이미지
│  └─ test/                                   # 단위·통합 테스트
├─ docs/                                      # 개발자가 관리하는 기준 문서
├─ db/init/                                   # Docker DB 최초 초기화 스크립트
├─ docker-compose.yml                         # 개발용 PostgreSQL + pgvector
├─ .env.example                               # 비밀값 없는 환경변수 예시
├─ run.ps1                                    # 로컬 실행 보조 스크립트
└─ pom.xml                                    # Maven 의존성·빌드 설정
```

## 도메인 내부 구조

```text
{domain}/
├─ api/controller/         # HTTP Controller
├─ api/dto/request/        # HTTP 요청 DTO
├─ api/dto/response/       # HTTP 응답 DTO
├─ api/mapper/             # Query Model → HTTP Response DTO
├─ command/application/    # 상태 변경 유스케이스
├─ query/application/      # 읽기 전용 유스케이스
├─ query/model/            # API에 의존하지 않는 조회 모델
├─ domain/                 # Entity, Enum, Repository, 도메인 규칙
├─ error/                  # 도메인별 ErrorCode
└─ infrastructure/         # 외부 시스템 구현체가 필요할 때만 생성
```

`user`, `care_relation`은 실제 구현이 있으며, 나머지 도메인의 빈 패키지는 기능을 시작할 공통 위치입니다. 빈 패키지를 임의로 삭제하거나 별도 최상위 Controller·Service·Repository 폴더를 만들지 않습니다.
