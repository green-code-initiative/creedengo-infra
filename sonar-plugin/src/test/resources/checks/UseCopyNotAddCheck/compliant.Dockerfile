FROM alpine:3.19
COPY ./app /usr/src/app
COPY --from=builder /out/bin /usr/local/bin/
RUN curl -fsSL -o /tmp/app.tar.gz https://example.com/app.tar.gz \
 && tar -xzf /tmp/app.tar.gz -C /opt/ \
 && rm /tmp/app.tar.gz

