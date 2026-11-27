# NIRVANA Auth Service

## 서비스 개요

- 목적: 계정, 인증 세션, 약관 및 마케팅 동의를 관리합니다.
- 담당자 / 백업 리뷰어: <!-- GitHub 사용자 또는 팀 -->
- 소유 데이터와 운영 책임: `nirvana_auth` 스키마
- API 문서: [Swagger UI](http://localhost:8081/swagger-ui/index.html)
- 기본 포트 / 헬스체크: `8081` / `GET /actuator/health`

## 로컬 데이터베이스

공용 MySQL 8.4 인스턴스에서 `nirvana_auth` 스키마를 사용합니다.
DB 프로젝트의 `DB/nirvana-app` 폴더에서 `docker compose -f docker-compose.auth-member.yml up -d --wait mysql`을 실행합니다.
기본 로컬 접속 주소는 `127.0.0.1:13306`이며 `DB_HOST`, `DB_PORT`, `DB_USERNAME`, `DB_PASSWORD`로 변경할 수 있습니다.
공용 Compose에서 설정한 MySQL 비밀번호를 현재 셸의 `DB_PASSWORD`에 지정한 뒤 Auth를 시작합니다. 기본 로컬 서버 포트는 `8081`이며 `SERVER_PORT`로 변경할 수 있습니다.

## 로컬 실행

- 사전 요구사항은 JDK 21입니다.
- macOS와 Linux에서는 `./gradlew bootRun`, Windows PowerShell에서는 `.\gradlew.bat bootRun`으로 실행합니다.
- 기본 `local` 프로필은 Config Server 없이 시작하며 Swagger UI를 활성화합니다. 다른 프로필에서는 API 문서와 Swagger UI가 기본으로 비활성화됩니다.
- Config Server 설정을 가져오려면 `SPRING_PROFILES_ACTIVE=config-server`를 설정합니다. 로컬 Swagger UI도 함께 쓰려면 `local,config-server`를 지정합니다. `CONFIG_SERVER_URL`의 기본값은 `http://localhost:8888`입니다.
- JAR 빌드는 `./gradlew bootJar`로 확인합니다. GitHub Actions는 `develop`과 `main` 대상 PR에서 `build`로 테스트와 문서 생성까지 검증합니다.

개발과 기여 방법은 [CONTRIBUTING.md](CONTRIBUTING.md)를 참고하세요.

## 로컬 이메일 회원가입 (Gmail)

Google 계정의 2단계 인증을 활성화하고 [앱 비밀번호](https://support.google.com/mail/answer/185833)를 발급합니다. IntelliJ 실행 설정이나 현재 셸에 아래 환경변수를 설정한 뒤 Auth를 시작합니다. 기존 MySQL 연결 설정도 필요합니다.

| 환경변수 | 값 |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `local,gmail-signup` |
| `GMAIL_SMTP_USERNAME` | 발신에 사용할 Gmail 주소 |
| `GMAIL_SMTP_APP_PASSWORD` | 해당 계정에서 발급한 앱 비밀번호 |

| 요청 | JSON 본문 | 성공 응답 |
| --- | --- | --- |
| `POST /signup/email-verification/send` | `{"email":"확인할 수 있는 테스트 메일 주소"}` | `204`. 가입할 수 있는 이메일이면 인증번호를 발송합니다. 이미 가입된 이메일은 `409 EMAIL_ALREADY_USED`를 반환합니다. |
| `POST /signup/email-verification/verify` | `{"email":"발송 요청에 사용한 주소","code":"받은 6자리 코드"}` | `200`, `data.verificationToken` |

인증번호는 발송 후 5분 동안 유효하며, 인증 증빙은 같은 만료 시각을 사용합니다. 로컬 테스트에서는 발송 후 1분이 지나야 재발송할 수 있고, 코드 확인에 5회 실패하면 새 코드를 받아야 합니다. 인증 상태는 Auth 프로세스 메모리에 저장되어 재시작하면 사라집니다. 로컬 Auth는 Gmail SMTP를 사용해 테스트 메일을 실제 주소로 보냅니다. 필수 약관에 동의한 뒤 발송·검증을 진행합니다. `POST /signup`에는 이메일, 인증 증빙(`verificationToken`), 나머지 가입 정보 및 필수 약관 동의 여부를 전달합니다. 가입 시 이메일 중복을 다시 확인합니다. 배포용 Notification 발송은 후속 단계입니다.

가입 요청에는 닉네임을 입력하지 않습니다. Auth는 MySQL에 `PENDING_REGISTRATION` 계정과 `ProfileCreationRequested` 이벤트를 함께 저장한 뒤 `202 Accepted`와 `data.userUuid`를 반환합니다. 응답은 가입 요청 접수를 뜻합니다. Auth는 Member의 `ProfileCreated` 이벤트를 받으면 대기 계정을 `ACTIVE`로 전환합니다.

### 가입 후 프로필 생성 이벤트

- 기존 Auth DDL의 `outboxEvents` 테이블을 사용합니다. 발행기는 Kafka 전송에 성공하면 이벤트를 `PUBLISHED`로 표시하고, 실패하면 `nextAttemptAt` 이후 다시 시도합니다.
- Kafka 연결은 `KAFKA_BOOTSTRAP_SERVERS`, 토픽은 `AUTH_USER_TOPIC`으로 설정합니다. 토픽 기본값은 `nirvana.auth.user`이고 파티션 키는 사용자 UUID입니다.
- 이벤트에는 `eventId`, `eventType`, `userUuid`, `name`, `countryCode`가 들어갑니다. Member가 이벤트를 받아 기본 닉네임과 프로필을 생성하는 계약입니다.
- 완료 이벤트 토픽은 양쪽에서 `MEMBER_PROFILE_COMPLETED_TOPIC`으로 설정합니다. 기본값은 `nirvana.member.profile-created`입니다. Auth 소비자 그룹 기본값은 `auth-profile-completion`이며 `AUTH_PROFILE_COMPLETION_GROUP_ID`로 변경할 수 있습니다.

입력값 검증에 실패하면 `400 INVALID_REQUEST`와 `error.details` 배열을 반환합니다. 각 항목의 `field`는 요청 JSON의 필드 이름이고, `code`는 검증 제약 이름, `message`는 표시할 문구입니다. 예를 들어 필수 약관에 동의하지 않으면 `requiredTermsAgreed`에 `AssertTrue` 오류가 내려갑니다.

## API 문서

- `local,gmail-signup` 프로필로 실행하면 Swagger UI에서 이메일 인증과 회원가입 API를 확인할 수 있습니다. OpenAPI JSON은 `/v3/api-docs`에서 제공합니다.
- `./gradlew restDocs` 또는 Windows에서 `.\gradlew.bat restDocs`를 실행하면 테스트를 거쳐 `build/docs/auth-api.md`를 생성합니다. 요청·응답 예시는 테스트 입력만 사용합니다.
