FROM maven:3.9-eclipse-temurin-17
COPY ./ ./
RUN mvn clean package
CMD ["java", "-jar", "target/app.jar"]

