FROM node:18-alpine
COPY . .
RUN npm install
COPY package*.json ./
WORKDIR /app
CMD ["node", "index.js"]

