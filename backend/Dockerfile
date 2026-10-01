# Stage 1: Build JAR bằng Gradle
FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /app

# Copy gradle wrapper & dependencies cache trước để tăng tốc build
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

# Copy mã nguồn và đóng gói
COPY src src
RUN ./gradlew bootJar --no-daemon -x test

# Stage 2: Runtime Image siêu nhẹ
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# Tạo user bảo mật không dùng root
RUN addgroup -S sqbgroup && adduser -S sqbuser -G sqbgroup
USER sqbuser

COPY --from=builder /app/build/libs/*.jar app.jar

ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseZGC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]