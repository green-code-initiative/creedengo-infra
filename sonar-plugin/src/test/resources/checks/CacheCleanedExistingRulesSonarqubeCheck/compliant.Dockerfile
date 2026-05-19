FROM python:3.11-alpine
RUN apk add --no-cache build-base
RUN pip install --no-cache-dir requests
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
RUN dnf install -y nginx && dnf clean all && rm -rf /var/cache/dnf
RUN composer install --no-dev && composer clear-cache

