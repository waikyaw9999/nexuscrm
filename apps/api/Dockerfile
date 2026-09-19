# Official Maven image: no Alpine apk (mirrors often fail on Docker Desktop).
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -q -B -DskipTests dependency:go-offline || true
COPY src src
RUN mvn -q -B -DskipTests package

# Ubuntu JRE already has bash, so healthchecks need no wget/curl install step.
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/target/nexuscrm-api-1.0.0.jar app.jar
COPY docker-healthcheck.sh /app/docker-healthcheck.sh
RUN chmod 755 /app/docker-healthcheck.sh
EXPOSE 8080
USER nobody
HEALTHCHECK --interval=10s --timeout=5s --start-period=40s --retries=12 \
  CMD ["/app/docker-healthcheck.sh"]
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
