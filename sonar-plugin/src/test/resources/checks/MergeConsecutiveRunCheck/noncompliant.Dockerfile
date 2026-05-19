FROM debian:12-slim
RUN apt-get update
RUN apt-get install -y curl
RUN apt-get install -y jq
COPY app /app
RUN chmod +x /app
RUN /app --selftest
CMD ["/app"]

