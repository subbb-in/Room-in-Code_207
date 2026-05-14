# 1. Build Stage
FROM gradle:jdk21-jammy AS builder
WORKDIR /app

# 빌드 설정 파일만 먼저 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

# 의존성 미리 다운로드
RUN chmod +x ./gradlew
RUN ./gradlew dependencies --no-daemon

# 주의. docker-compose.yml context 설정 be로 지정
COPY . .
RUN chmod +x ./gradlew
RUN ./gradlew clean build -x test --no-daemon

# 2. Run Stage
FROM eclipse-temurin:21-jdk-jammy
WORKDIR /app

# plain.jar 제외한 실행용 jar만 가져옴
COPY --from=builder /app/build/libs/*-SNAPSHOT.jar app.jar

# 컨테이너 뜨자마자 java -jra app.jar 명령어 강제 실행
# 컨테이너가 exe처럼 동작하도록
ENTRYPOINT ["java", "-jar", "app.jar"]
