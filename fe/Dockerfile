# 1. 빌드
FROM node:24.13.0-slim AS build-stage

# 2. 작업 공간 지정
WORKDIR /app

#ENV NODE_ENV=development
#ENV NODE_ENV=production

# 의존성 설치 속도를 높이기 위해 캐시 활
COPY package*.json ./
RUN npm ci && npm cache clean --force

# fe 폴더 모든 소스를 /app으로 복사
COPY . .

# Vite 빌드 수행 -> app/dist에 결과물 생성
RUN npm run build

# 2. 배포용
FROM nginx:stable-alpine

# fe/nginx.conf를 vue-container 내부 nginx conf 경로로 복사
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 빌드된 결과물(app/dist)만 vue-container 내부 /usr/share/nginx/html 경로로 복사
COPY --from=build-stage /app/dist /usr/share/nginx/html

# 컨테이너 내부 80 포트 사용
EXPOSE 80

# Nginx 실행
CMD ["nginx", "-g", "daemon off;"]