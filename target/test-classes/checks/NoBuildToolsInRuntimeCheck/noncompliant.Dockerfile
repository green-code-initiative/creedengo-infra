FROM eclipse-temurin:17-jdk-alpine AS builder
RUN apk add --no-cache gcc make
COPY . /src
RUN cd /src && ./build.sh

FROM eclipse-temurin:17-jre-alpine
RUN apk add --no-cache gcc make musl-dev git
COPY --from=builder /src/app.jar /app.jar
RUN pip install poetry
CMD ["java", "-jar", "/app.jar"]

