# --- Build stage ---
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# Copy Maven wrapper and pom first for dependency caching
COPY mvnw mvnw
COPY .mvn .mvn
COPY pom.xml pom.xml
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy source and build (skip tests — they run in CI)
COPY src src
RUN ./mvnw package -DskipTests -B && \
    mv target/auth-service-*.jar target/auth-service.jar

# --- Runtime stage ---
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
COPY --from=build /app/target/auth-service.jar auth-service.jar
USER appuser

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "auth-service.jar"]
