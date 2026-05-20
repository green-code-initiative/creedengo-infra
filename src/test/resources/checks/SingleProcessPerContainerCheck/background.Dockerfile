FROM debian:12-slim
COPY start.sh /usr/local/bin/start.sh
ENTRYPOINT ["sh", "-c", "/usr/local/bin/worker & /usr/local/bin/server"]

