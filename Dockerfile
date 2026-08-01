# syntax=docker/dockerfile:1

FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /workspace

ARG SERVICE

RUN case "${SERVICE}" in \
        api-gateway|identity-service|academic-service|enrollment-service|finance-service|notification-service) ;; \
        *) echo "Unsupported service: ${SERVICE}" && exit 1 ;; \
    esac

COPY pom.xml ./

COPY services/api-gateway/pom.xml \
     services/api-gateway/pom.xml

COPY services/identity-service/pom.xml \
     services/identity-service/pom.xml

COPY services/academic-service/pom.xml \
     services/academic-service/pom.xml

COPY services/enrollment-service/pom.xml \
     services/enrollment-service/pom.xml

COPY services/finance-service/pom.xml \
     services/finance-service/pom.xml

COPY services/notification-service/pom.xml \
     services/notification-service/pom.xml

RUN mvn \
    -B \
    -ntp \
    -pl "services/${SERVICE}" \
    -am \
    dependency:go-offline

COPY services ./services

RUN mvn \
    -B \
    -ntp \
    -pl "services/${SERVICE}" \
    -am \
    -DskipTests \
    package \
    && JAR_FILE="$(find "services/${SERVICE}/target" \
        -maxdepth 1 \
        -type f \
        -name '*.jar' \
        ! -name '*-plain.jar' \
        | head -n 1)" \
    && test -n "${JAR_FILE}" \
    && cp "${JAR_FILE}" /workspace/application.jar

FROM eclipse-temurin:21-jre-ubi9-minimal AS runtime

WORKDIR /app

COPY --from=builder \
     --chown=10001:0 \
     /workspace/application.jar \
     /app/application.jar

USER 10001

ENTRYPOINT ["java", "-jar", "/app/application.jar"]