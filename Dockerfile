FROM eclipse-temurin:25-jdk AS builder
WORKDIR /opt/app

ARG PRIMEUI_LICENSE=""
ENV PRIMEUI_LICENSE=$PRIMEUI_LICENSE

COPY gradle/ gradle/
COPY gradlew build.gradle.kts settings.gradle.kts gradle.properties ./

COPY backend/ backend/
COPY frontend/ frontend/

RUN chmod +x ./gradlew
RUN ./gradlew :backend:bootJar -x test --no-daemon

FROM eclipse-temurin:25-jre
WORKDIR /opt/app

RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

COPY --from=builder /opt/app/backend/build/libs/*.jar /opt/app/app.jar
EXPOSE 8080

HEALTHCHECK --interval=5s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /opt/app/app.jar"]