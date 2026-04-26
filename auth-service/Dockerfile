# Lightweight single-stage image — expects the JAR to be pre-built.
# Build the JAR first: ./mvnw package -DskipTests -B
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
COPY target/auth-service-0.0.1-SNAPSHOT.jar auth-service.jar
USER appuser

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "auth-service.jar"]
