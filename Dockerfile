
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copy file cấu hình Maven trước để cache dependencies 
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy toàn bộ mã nguồn và build file JAR
COPY src ./src
RUN mvn clean package -DskipTests


FROM eclipse-temurin:21-jre-jammy
WORKDIR /app


COPY --from=build /app/target/game2048-backend-0.0.1-SNAPSHOT.jar app.jar

# Mở cổng 8080 (mặc định của Spring Boot)
EXPOSE 8080


ENTRYPOINT ["java", \
            "-Xmx350m", \
            "-Xms256m", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "-jar", "app.jar", \
            "--server.port=${PORT:8080}"]
