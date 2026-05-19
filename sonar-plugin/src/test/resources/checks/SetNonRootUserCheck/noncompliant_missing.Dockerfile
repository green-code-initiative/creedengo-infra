FROM eclipse-temurin:17-jre-alpine
COPY app.jar /app.jar
CMD ["java", "-jar", "/app.jar"]

