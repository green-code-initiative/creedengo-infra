FROM debian:12-slim
RUN apt-get update && apt-get install -y supervisor nginx php-fpm
COPY supervisord.conf /etc/supervisor/conf.d/app.conf
CMD ["/usr/bin/supervisord", "-c", "/etc/supervisor/conf.d/app.conf"]

