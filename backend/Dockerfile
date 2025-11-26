FROM gradle:8.14.3-jdk21 AS builder

WORKDIR /app/
COPY build.gradle.kts settings.gradle.kts gradle .

ENV GRADLE_TARGET=bootJar

# We've got caching at home
RUN gradle --no-daemon ${GRADLE_TARGET} || true

COPY . .
#TODO: make optional
RUN gradle --no-daemon ${GRADLE_TARGET}




FROM amazoncorretto:21-alpine AS runner

WORKDIR /app

RUN apk update && apk add --no-cache python3 py3-pip
RUN python3 -m venv /opt/venv
ENV PATH="/opt/venv/bin:${PATH}"
RUN pip install lizard

COPY --from=builder /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
EXPOSE 8080
STOPSIGNAL SIGINT
