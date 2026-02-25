FROM bitnamilegacy/java:17.0.12-10

WORKDIR /app
COPY ./opentheso.jar .
COPY ./application-prod.yaml .
COPY ./logback-spring.xml .

EXPOSE 8099
CMD ["java", "-jar", "-Dlogging.config=./logback-spring.xml", "opentheso.jar", "--spring.profiles.active=prod", "--spring.config.additional-location=application-prod.yaml"]