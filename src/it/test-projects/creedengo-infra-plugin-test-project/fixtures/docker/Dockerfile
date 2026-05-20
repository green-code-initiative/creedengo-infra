# Single-stage Dockerfile running a build step → triggers MultistageCheck (GCI1025).
# No explicit USER → triggers SetNonRootUserCheck (GCI1035, MESSAGE_MISSING).
FROM eclipse-temurin:17
WORKDIR /app
COPY . .
RUN mvn clean package
EXPOSE 8080
CMD ["java", "-jar", "/app/app.jar"]

