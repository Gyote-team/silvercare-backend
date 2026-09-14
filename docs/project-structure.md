# 🗂️ SilverCare Backend 폴더 구조

이 문서는 현재 Spring Boot 백엔드의 파일·패키지 배치와 각 영역의 책임을 빠르게 파악하기 위한 안내입니다.

```text
backend/
├─ src/
│  ├─ main/
│  │  ├─ java/com/gyote/silvercare/
│  │  │  ├─ SilverCareApplication.java        # Spring Boot 실행 진입점
│  │  │  ├─ global/                           # 전 도메인 공통 기술 영역
│  │  │  │  ├─ auth/                          # Kakao OAuth2, JWT, 쿠키, 인증 필터
│  │  │  │  ├─ config/                        # Security, CORS, 환경 설정
│  │  │  │  ├─ exception/                     # 공통 예외·오류 응답
│  │  │  │  └─ web/                           # 공통 웹 진입점·리다이렉트
│  │  │  ├─ user/                             # 회원·역할
│  │  │  │  ├─ api/controller/                # HTTP Controller
│  │  │  │  ├─ api/dto/request/               # HTTP 요청 DTO
│  │  │  │  ├─ api/dto/response/              # HTTP 응답 DTO
│  │  │  │  ├─ api/mapper/                    # Query Model → HTTP DTO
│  │  │  │  ├─ command/application/           # 회원 생성·역할 선택 등 상태 변경
│  │  │  │  ├─ query/application/             # 내 정보 등 읽기 전용 조회
│  │  │  │  └─ domain/                        # User Entity, Enum, Repository
│  │  │  ├─ care_relation/                    # 개인-보호자 연결
│  │  │  │  ├─ api/controller/                # 연결 관리 API
│  │  │  │  ├─ api/dto/request/               # 연결 요청 DTO
│  │  │  │  ├─ api/dto/response/              # 연결 응답 DTO
│  │  │  │  ├─ api/mapper/                    # Query Model → HTTP DTO
│  │  │  │  ├─ command/application/           # 요청·수락·거절·취소·해제
│  │  │  │  ├─ query/application/             # 연결 목록 조회
│  │  │  │  ├─ query/model/                   # API에 의존하지 않는 조회 모델
│  │  │  │  ├─ domain/                        # CareRelation Entity, 상태, Repository
│  │  │  │  └─ error/                         # 도메인별 오류 코드
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
├─ docs/
│  ├─ architecture-and-data.md                # CQRS·데이터·권한 설계
│  ├─ development-convention.md               # 패키지·예외 처리 규약
│  ├─ project-structure.md                    # 전체 폴더 구조
│  └─ team-development-guide.md               # 팀원 실행·개발·PR 안내
├─ db/init/                                   # Docker DB 최초 초기화 스크립트
├─ docker-compose.yml                         # 개발용 PostgreSQL + pgvector
├─ .env.example                               # 비밀값 없는 환경변수 예시
├─ run.ps1                                    # 로컬 실행 보조 스크립트
└─ pom.xml                                    # Maven 의존성·빌드 설정
```

## 🔁 도메인 내부의 공통 패턴

```text
{domain}/
├─ api/controller/         HTTP 요청을 받고 application Service를 호출하는 Controller
├─ api/dto/request/        HTTP 요청 DTO
├─ api/dto/response/       HTTP 응답 DTO
├─ api/mapper/             Query Model을 HTTP 응답 DTO로 변환
├─ command/application/    생성·수정·삭제·상태 변경과 트랜잭션
├─ query/application/      데이터 변경 없이 조회하고 화면용 응답을 조립
├─ query/model/            API에 의존하지 않는 읽기 전용 모델
├─ domain/                 Entity, Enum, Repository, 핵심 도메인 규칙
├─ error/                  도메인별 ErrorCode
└─ infrastructure/         외부 시스템 구현체가 필요할 때만 생성
```

`api`는 HTTP 규약만 담당하고, 실제 비즈니스 흐름은 `application` 서비스에 둡니다. Query는 `query/model`을 반환하며 API DTO 변환은 `api/mapper`가 맡습니다. Command와 Query를 분리해 쓰기 로직과 읽기 로직이 섞이지 않도록 하되, 현재 단계에서는 동일한 PostgreSQL DB를 함께 사용합니다.

## 🤖 AI 서버와의 경계

OCR, STT, LLM, RAG는 이 저장소에 포함하지 않습니다. Spring 백엔드는 사용자·개인-보호자 연결 권한·파일 메타데이터와 AI 서버 연동 규약을 담당하며, 실제 AI 처리는 별도 Python 저장소에서 관리합니다. 보호자 열람은 `care_relation`이 `ACTIVE`인 경우에만 허용하며, 별도 `consent` 패키지는 두지 않습니다.
