# 1단계: Gradle 빌드 (Java 21 JDK)
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Gradle Wrapper 및 빌드 파일 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN chmod +x ./gradlew

# 소스 코드 복사 및 실행 가능한 JAR 파일 생성
COPY src src
RUN ./gradlew bootJar --no-daemon -x test

# 2단계: 실행 환경 (Java 21 JRE)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Docker Compose healthcheck용 curl 설치
RUN apk add --no-cache curl

# 빌드 결과물 복사
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]