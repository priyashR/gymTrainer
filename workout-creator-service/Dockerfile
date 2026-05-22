# Lightweight single-stage image — expects the JAR to be pre-built.
# Build the JAR first: mvn package -DskipTests -B
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
COPY target/workout-creator-service-0.0.1-SNAPSHOT.jar workout-creator-service.jar
USER appuser

EXPOSE 8082
ENTRYPOINT ["java", "-jar", "workout-creator-service.jar"]
