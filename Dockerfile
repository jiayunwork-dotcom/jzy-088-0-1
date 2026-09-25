# syntax=docker/dockerfile:1

# ---- build stage: Maven 3.9 on Temurin JDK 17 (mainline, non-archived tag) ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Resolve dependencies first for better layer caching.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---- runtime stage: plain JRE 17, single container serves HTTP on 8080 ----
FROM eclipse-temurin:17-jre
WORKDIR /app

RUN groupadd --system bem && useradd --system --gid bem --no-create-home bem
COPY --from=build /build/target/*.jar /app/app.jar
USER bem

EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD ["bash", "-c", "exec 3<>/dev/tcp/127.0.0.1/8080 && printf 'GET /api/example HTTP/1.0\\r\\n\\r\\n' >&3 && head -n1 <&3 | grep -q '200'"]

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
