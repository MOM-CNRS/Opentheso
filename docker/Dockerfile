FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /src
COPY pom.xml .
COPY src ./src
# ponytail: BuildKit cache mount instead of a separate go-offline layer — same warm rebuilds, one RUN
RUN --mount=type=cache,target=/root/.m2 mvn -B -DskipTests package

FROM eclipse-temurin:17-jre

WORKDIR /app
COPY --from=build /src/target/opentheso-*.jar ./opentheso.jar
COPY docker/application-prod.yaml docker/logback-spring.xml ./

EXPOSE 8099
CMD ["java", "-jar", "-Dlogging.config=./logback-spring.xml", "opentheso.jar", "--spring.profiles.active=prod", "--spring.config.additional-location=application-prod.yaml"]
