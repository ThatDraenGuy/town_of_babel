FROM gradle:8.14.3-jdk21 AS builder

WORKDIR /app/
COPY build.gradle.kts settings.gradle.kts gradle .

ENV GRADLE_TARGET=bootJar

# We've got caching at home
RUN gradle --no-daemon ${GRADLE_TARGET} || true

COPY . .
RUN gradle --no-daemon ${GRADLE_TARGET}

FROM amazoncorretto:21-alpine AS runner

COPY --from=builder /app/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
