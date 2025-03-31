FROM amazoncorretto:17.0.7-alpine as builder
WORKDIR /app

# 필요한 의존성 설치
RUN apk add --no-cache bash

COPY . .

# gradlew에 실행 권한 부여
RUN chmod +x ./gradlew

# 실행 가능한 jar 파일만 생성
RUN ./gradlew clean build -x test

FROM amazoncorretto:17.0.7-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

