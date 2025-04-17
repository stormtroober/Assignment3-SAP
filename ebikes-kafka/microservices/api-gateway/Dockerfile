FROM gradle:8.11-jdk21 AS build
COPY app/build.gradle.kts settings.gradle.kts gradle.properties ./
COPY app/src src
RUN --mount=type=cache,target=/home/gradle/.gradle/caches gradle bootJar --no-daemon --parallel --build-cache

FROM openjdk:21-jdk-slim

RUN apt-get update && apt-get install -y curl

WORKDIR /app

COPY --from=build /home/gradle/build/libs/api-gateway-1.0.0-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]