# Dockerfile for AidBridge Spring Boot Backend
# Sử dụng JDK 25 (Temurin) để build và chạy ứng dụng
FROM eclipse-temurin:25-jdk as builder
WORKDIR /app
COPY . .
RUN ./gradlew build --no-daemon

# Tạo image chạy ứng dụng từ build output
FROM eclipse-temurin:25-jdk
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
# Chỉnh sửa tên file JAR nếu cần thiết
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
