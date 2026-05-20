FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S app && adduser -S app -G app
COPY --chown=app:app app.jar /app.jar
USER app:app
CMD ["java", "-jar", "/app.jar"]

