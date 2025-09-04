# 1단계: Maven 빌드
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# 종속성 캐싱을 위해 pom.xml 먼저 복사
COPY pom.xml .
RUN mvn dependency:go-offline

# 소스 복사 & 빌드
COPY src ./src
RUN mvn clean package -DskipTests

# 2단계: 실행 전용 이미지
FROM eclipse-temurin:17-jdk
WORKDIR /app

# 빌드 결과물만 복사
COPY --from=build /app/target/*.jar app.jar

# Spring Boot 실행 포트
EXPOSE 8081

# 실행 명령
ENTRYPOINT ["java", "-jar", "app.jar"]
