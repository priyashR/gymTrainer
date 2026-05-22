# Lightweight single-stage image — expects the JAR to be pre-built.
# Build the JAR first: ./mvnw package -DskipTests -B
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
COPY target/workout-session-service-0.0.1-SNAPSHOT.jar workout-session-service.jar
USER appuser

EXPOSE 8083
ENTRYPOINT ["java", "-jar", "workout-session-service.jar"]
