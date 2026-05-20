# Multi-stage builder (intermediate) is irrelevant — only the last stage is checked.
FROM maven:3.9-eclipse-temurin-17 AS builder
RUN mvn package

FROM eclipse-temurin:17-jre-alpine
CMD ["java", "-jar", "/app.jar"]

