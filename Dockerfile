# 빌드 스테이지
FROM gradle:7.6-jdk17 as builder
WORKDIR /app
COPY . .
RUN gradle build --no-daemon

# 실행 스테이지
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
