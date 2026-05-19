FROM maven:3.9-eclipse-temurin-17 AS builder
COPY ./ ./
RUN mvn -B -ntp clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
COPY --from=builder /target/app.jar /app.jar
CMD ["java", "-jar", "/app.jar"]

