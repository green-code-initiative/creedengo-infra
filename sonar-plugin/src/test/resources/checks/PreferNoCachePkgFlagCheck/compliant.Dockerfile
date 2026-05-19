FROM alpine:3.19
RUN apk add --no-cache curl jq
RUN pip install --no-cache-dir requests
RUN npm install --no-cache express

