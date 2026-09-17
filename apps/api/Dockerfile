FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /src
RUN apk add --no-cache maven
COPY pom.xml .
RUN mvn -q -B -DskipTests dependency:go-offline || true
COPY src src
RUN mvn -q -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN apk add --no-cache wget
COPY --from=build /src/target/nexuscrm-api-1.0.0.jar app.jar
EXPOSE 8080
USER nobody
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
