# Bước 1: Build ứng dụng bằng Maven và Java 21
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copy file cấu hình Maven trước để cache dependencies (tăng tốc độ build lần sau)
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy toàn bộ mã nguồn và build file JAR
COPY src ./src
RUN mvn clean package -DskipTests

# Bước 2: Chạy ứng dụng với JRE 21 nhẹ hơn
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copy file .jar đã build từ bước 1
# Lưu ý: artifactId của bạn là game2048-backend, nên file tạo ra sẽ có tên này
COPY --from=build /app/target/game2048-backend-0.0.1-SNAPSHOT.jar app.jar

# Mở cổng 8080 (mặc định của Spring Boot)
EXPOSE 8080

# Các tham số tối ưu cho Java 21 trên môi trường Cloud có RAM thấp (Gói Free)
# -Xmx350m: Giới hạn bộ nhớ Heap tối đa 350MB (để dành 162MB cho hệ thống nếu gói 512MB)
# -Djava.security.egd: Giúp khởi động nhanh hơn
ENTRYPOINT ["java", \
            "-Xmx350m", \
            "-Xms256m", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "-jar", "app.jar", \
            "--server.port=${PORT:8080}"]