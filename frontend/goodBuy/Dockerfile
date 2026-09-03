# Node.js 22버전 기반 (engines 조건: ^20.19.0 || >=22.12.0 충족)
FROM node:22-alpine

WORKDIR /app

# 패키지 명세서 복사 및 의존성 설치
COPY package*.json ./
RUN npm install

# 소스 코드 전체 복사
COPY . .

EXPOSE 5173

# Vite 개발 서버 실행 (외부 컨테이너 접속 허용)
CMD ["npm", "run", "dev", "--", "--host", "0.0.0.0"]