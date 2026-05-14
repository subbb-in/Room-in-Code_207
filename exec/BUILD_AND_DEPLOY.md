# 빌드 및 배포 가이드

본 문서는 GitLab 소스 클론 이후 프로젝트를 빌드하고 배포하기 위한 환경 설정 및 절차를 안내합니다.

## 1. 개발/운영 환경 및 제품 정보

| 구분 | 제품명 / 도구 | 버전 / 설정 | 비고 |
| --- | --- | --- | --- |
| **운영체제** | Windows / Linux (Docker) | - | Docker 기반 배포 권장 |
| **JVM** | OpenJDK (Eclipse Temurin) | 21 (LTS) | `be/build.gradle` 및 `be/Dockerfile` 참조 |
| **Web Server** | Nginx | stable-alpine | `fe/Dockerfile` 및 `nginx.conf` 참조 |
| **WAS** | Apache Tomcat | 10.x (Spring Boot 내장) | Spring Boot 3.5.10 |
| **Backend Framework** | Spring Boot | 3.5.10 | Gradle 빌드 |
| **Frontend Framework** | Vue.js | 3.x (Vite) | Node.js 20+ (Docker: 24.13.0) |
| **IDE** | IntelliJ IDEA | 최신 버전 | Backend 개발용 |
| **IDE** | VS Code | 최신 버전 | Frontend 개발용 |
| **Database** | MySQL | 8.0.43 | UTF8MB4, Asia/Seoul Timezone |
| **Cache/Session** | Redis | 7.2 | - |
| **Vector DB** | Qdrant | latest | AI 기능용 |

## 2. 빌드 및 배포 환경 변수

프로젝트 빌드 및 실행 시 필요한 환경 변수입니다. 배포 시 `.env` 파일 또는 시스템 환경 변수로 설정해야 합니다.

### Backend (Spring Boot)
`be` 컨테이너 또는 `application.properties` 에서 참조하는 변수들입니다.

| 변수명 | 설명 | 예시 / 기본값 |
| --- | --- | --- |
| `DB_URL` | MySQL 접속 URL | `jdbc:mysql://localhost:3306/ssafy_hamcheese?serverTimezone=Asia/Seoul&characterEncoding=UTF-8` |
| `DB_USERNAME` | DB 접속 계정 | `hamcheese` |
| `DB_PASSWORD` | DB 접속 비밀번호 | `***MASKED***` |
| `DB_DATABASE` | 사용할 데이터베이스 명 | `ssafy_hamcheese` |
| `SPRING_DATA_REDIS_HOST` | Redis 호스트 | `redis-container` |
| `SPRING_DATA_REDIS_PORT` | Redis 포트 | `6379` |
| `JWT_SECRET_KEY` | JWT 서명 비밀키 | `***MASKED***` |
| `AI_SERVER_URL` | AI 서버 URL | `http://ai-container:8000` |
| `JUDGE_URL` | Judge0 채점 서버 URL | `http://13.55.30.182:2358` |
| `BASE_URL` | 백엔드 API 기본 URL | `http://54.180.246.233:8080` |
| `FRONT_BASE_URL` | 프론트엔드 URL (CORS/Redirect) | `https://roomincode.site` |
| `MAIL_PASSWORD` | Google SMTP 앱 비밀번호 | `***MASKED***` |
| `PAPAGO_CLIENT_ID` | 네이버 Papago API ID | `tfwp2uekju` |
| `PAPAGO_CLIENT_SECRET` | 네이버 Papago API Secret | `***MASKED***` |
| `PAPAGO_URL` | Papago API URL | `https://papago.apigw.ntruss.com/` |
| `GITHUB_CLIENT_ID` | GitHub OAuth Client ID | `***MASKED***` |
| `GITHUB_CLIENT_SECRET`| GitHub OAuth Client Secret | `***MASKED***` |
| `GOOGLE_CLIENT_ID` | Google OAuth Client ID | `***MASKED***` |
| `GOOGLE_CLIENT_SECRET`| Google OAuth Client Secret | `***MASKED***` |
| `OPENVIDU_URL` | OpenVidu 서버 URL | `http://localhost:4443/` (미사용 - TODO: NOT USED) |
| `OPENVIDU_SECRET` | OpenVidu 시크릿 | `MY_SECRET` (미사용 - TODO: NOT USED) |
| `FILE_UPLOAD_DIR` | 파일 업로드 경로 | `/home/ubuntu/uploads` |

### AI Server (FastAPI)
`ai` 컨테이너에서 사용하는 환경 변수입니다.

| 변수명 | 설명 | 예시 |
| --- | --- | --- |
| `DB_HOST` | MySQL 호스트 | `mysql-container` |
| `DB_PORT` | MySQL 포트 | `3306` |
| `DB_USER` | DB 사용자 | `${DB_USERNAME}` |
| `DB_PASSWORD` | DB 비밀번호 | `${DB_PASSWORD}` |
| `DB_NAME` | DB 이름 | `${DB_DATABASE}` |
| `ANTHROPIC_API_KEY` | Anthropic (Claude) API 키 | `***MASKED***` |
| `ANTHROPIC_BASE_URL` | Anthropic Base URL | `https://gms.ssafy.io/gmsapi/api.anthropic.com` |
| `QDRANT_HOST` | Qdrant 호스트 | `qdrant-container` |
| `QDRANT_PORT` | Qdrant 포트 | `6333` |
| `HF_TOKEN` | HuggingFace Token | `***MASKED***` |

### Frontend (Vue.js)
빌드 시점(`npm run build`)에 주입되거나, 런타임에 활용되는 변수입니다.

| 변수명 | 설명 |
| --- | --- |
| `VITE_API_BASE` | 백엔드 API 주소 | `https://roomincode.site/` |
| `VITE_IMG_BASE` | 이미지 경로 기본 URL | `https://roomincode.site/images/` |
| `VITE_WS_BASE` | WebSocket 기본 URL | `https://roomincode.site/ws` |

## 3. 배포 시 특이사항

1.  **Docker Compose 기반 배포**
    *   `docker-compose.yml` 파일이 프로젝트 루트에 위치해 있습니다.
    *   `docker-compose up -d --build` 명령어로 전체 서비스를 빌드 및 실행할 수 있습니다.
    *   네트워크: `roomincode-network`라는 외부 네트워크를 사용하도록 설정되어 있으므로, 사전에 `docker network create roomincode-network` 명령어로 네트워크를 생성해야 할 수 있습니다. (또는 `external: true` 제거 후 내부 네트워크 사용)

2.  **데이터 초기화**
    *   MySQL: `./common/data/init` 폴더가 `/docker-entrypoint-initdb.d/`에 마운트되어 있어, 컨테이너 최초 실행 시 초기 SQL이 자동 실행됩니다.
    *   Uploads: `./uploads` 폴더가 컨테이너의 `/app/uploads`와 바인딩되어 있으므로, 호스트에 해당 폴더를 생성하고 권한을 확인해야 합니다 (Permission Denied 방지).

3.  **Qdrant 저장소**
    *   호스트 경로 `/home/ubuntu/app/qdrant_storage`가 컨테이너의 `/qdrant/storage`에 마운트되어 있습니다. 배포 서버 경로에 맞춰 `docker-compose.yml` 수정이 필요할 수 있습니다.

4.  **AI 서버 빌드**
    *   `ai` 폴더 내에 `requirements.txt`가 존재하며, Docker 빌드 시 의존성을 설치합니다.
    *   특수 하드웨어(GPU) 사용 시 `nvidia-docker` 설정 등이 필요할 수 있으나, 현재 설정은 CPU 기반으로 보입니다 (`python:3.10.11-slim`).

## 4. 데이터베이스 및 주요 프로퍼티 파일 목록

### 1) 데이터베이스 접속 정보
*   **파일 위치**: `docker-compose.yml` (환경 변수 주입), `be/src/main/resources/application.properties`
*   **주요 키**: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
*   **ERD 관련**: 프로젝트 내 명시적인 ERD 파일은 발견되지 않았으나, `be/src/main/resources/mappers` 내의 MyBatis XML 파일이나 `common/data/init` 내의 SQL 파일(존재 시)을 통해 스키마 확인이 가능합니다.

### 2) 주요 프로퍼티 정의 파일
| 파일 경로 | 용도 | 포함 정보 |
| --- | --- | --- |
| `be/src/main/resources/application.properties` | Spring Boot 메인 설정 | DB, Redis, Mail, JWT, OAuth, 외부 API 설정 |
| `docker-compose.yml` | 컨테이너 오케스트레이션 설정 | 서비스별 환경 변수 매핑, 포트, 볼륨 설정 |
| `fe/nginx.conf` | Web Server 설정 | Nginx 라우팅 및 프록시 설정 |
| `fe/.env` (또는 `.env.production`) | Frontend 환경 변수 | API Base URL 등 (Git에 포함되지 않았을 수 있음) |
| `ai/Dockerfile` & `requirements.txt` | AI 서버 설정 | Python 환경 및 의존성 |
