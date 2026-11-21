# 서비스 이름

## 서비스 개요

- 목적: <!-- 이 서비스가 맡는 기능 -->
- 담당자 / 백업 리뷰어: <!-- GitHub 사용자 또는 팀 -->
- 소유 데이터와 운영 책임: <!-- 서비스 경계와 책임 -->
- API 문서: [Swagger UI](http://localhost:8080/swagger-ui/index.html)
- 기본 포트 / 헬스체크: `8080` / `GET /actuator/health`

## 로컬 실행

- 사전 요구사항은 JDK 21입니다.
- macOS와 Linux에서는 `./gradlew bootRun`, Windows PowerShell에서는 `.\gradlew.bat bootRun`으로 실행합니다.
- 기본 `local` 프로필은 Config Server 없이 시작하며 Swagger UI를 활성화합니다. 다른 프로필에서는 API 문서와 Swagger UI가 기본으로 비활성화됩니다.
- Config Server 설정을 가져오려면 `SPRING_PROFILES_ACTIVE=config-server`를 설정합니다. 로컬 Swagger UI도 함께 쓰려면 `local,config-server`를 지정합니다. `CONFIG_SERVER_URL`의 기본값은 `http://localhost:8888`입니다.
- JAR 빌드는 `./gradlew bootJar`로 확인합니다. GitHub Actions도 `develop`과 `main` 대상 PR에서 이 빌드를 실행합니다.

## 서비스 레포 생성 후

- `settings.gradle`의 프로젝트 이름, 시작 클래스의 패키지, `spring.application.name`을 서비스에 맞게 변경합니다.
- 애플리케이션 이름을 Config Repo의 서비스별 설정 파일 이름과 일치시키고 포트와 담당자를 README에 기록합니다.

개발과 기여 방법은 [CONTRIBUTING.md](CONTRIBUTING.md)를 참고하세요.
