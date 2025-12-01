FROM gradle:8.14.3-jdk21 AS builder

WORKDIR /app/
COPY build.gradle.kts settings.gradle.kts gradle .

ENV GRADLE_TARGET=bootJar

# We've got caching at home
RUN gradle --no-daemon ${GRADLE_TARGET} || true

COPY . .
RUN gradle --no-daemon ${GRADLE_TARGET}

FROM amazoncorretto:21-alpine AS runner

WORKDIR /app

COPY requirements.txt .
RUN apk add --no-cache python3 py3-pip && \
    pip install --no-cache-dir --break-system-packages -r requirements.txt && \
    apk del py3-pip && \
    apk cache purge

COPY --from=builder /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
EXPOSE 8080
STOPSIGNAL SIGINT
