FROM eclipse-temurin:17-jdk-alpine-3.23 AS build

WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -DskipTests dependency:go-offline
COPY src src
RUN ./mvnw -B -DskipTests package

FROM eclipse-temurin:17-jdk-alpine-3.23

RUN apk add --no-cache docker-cli \
    && addgroup -S -g 10001 devedu \
    && adduser -S -D -H -u 10001 -G devedu devedu \
    && mkdir -p /app /judge-workspaces \
    && touch /judge-workspaces/.initialized \
    && chown -R devedu:devedu /app /judge-workspaces

WORKDIR /app
COPY --from=build --chown=devedu:devedu /workspace/target/learning-platform-*.jar app.jar
USER devedu

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
