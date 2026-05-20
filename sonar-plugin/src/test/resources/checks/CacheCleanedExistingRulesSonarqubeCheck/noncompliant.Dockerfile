FROM debian:bookworm-slim
RUN apt-get update && apt-get install -y curl
RUN dnf install -y nginx
RUN pip install requests
RUN composer install
RUN gem install bundler

