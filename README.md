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
- JAR 빌드는 `./gradlew bootJar`로 확인합니다. GitHub Actions도 `develop`과 `main` 대상 PR에서 이 빌드를 실행합니다.

개발과 기여 방법은 [CONTRIBUTING.md](CONTRIBUTING.md)를 참고하세요.
