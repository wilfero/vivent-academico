# syntax=docker/dockerfile:1

FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /app

# Maven Wrapper needs these tools to download and unpack Maven inside the build container.
RUN apk add --no-cache curl unzip

COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -DskipTests dependency:go-offline

COPY src src
COPY database database
RUN ./mvnw -B -DskipTests package

FROM eclipse-temurin:25-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S vivent && adduser -S vivent -G vivent

ENV JAVA_OPTS="" \
    SPRING_DOCKER_COMPOSE_ENABLED=false

COPY --from=build /app/target/*.jar /app/app.jar

USER vivent
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
