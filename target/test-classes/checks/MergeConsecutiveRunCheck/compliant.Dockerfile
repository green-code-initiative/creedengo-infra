# Compliant: a single merged RUN per stage.
FROM debian:12-slim
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl jq \
 && rm -rf /var/lib/apt/lists/*
COPY app /app
CMD ["/app"]

