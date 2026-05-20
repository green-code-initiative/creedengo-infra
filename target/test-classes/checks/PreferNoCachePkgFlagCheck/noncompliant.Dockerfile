FROM alpine:3.19
RUN apk add curl jq
RUN pip install requests
RUN npm install express

