# Multi-stage: builder may install gcc/make, runtime stays slim.
FROM eclipse-temurin:17-jdk-alpine AS builder
RUN apk add --no-cache gcc make musl-dev
COPY . /src
RUN cd /src && ./build.sh

FROM eclipse-temurin:17-jre-alpine
COPY --from=builder /src/app.jar /app.jar
RUN apk add --no-cache tini
CMD ["java", "-jar", "/app.jar"]

