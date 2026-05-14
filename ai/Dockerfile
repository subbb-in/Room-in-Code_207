# 1. Build Stage
FROM python:3.11-slim AS builder

# 작업 디렉토리 설정
WORKDIR /app

# AI 관련 라이브러리 컴파일용 도구 설치
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

# 의존성 설치
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
RUN pip install datasets pandas

# 2. Run Stage
FROM python:3.11-slim

WORKDIR /app

# ROOT 권한으로 복사한 라이브러리 파일을 다른 사용자가 읽지 못할 수도
COPY --from=builder /usr/local/lib/python3.11/site-packages /usr/local/lib/python3.11/site-packages
COPY --from=builder /usr/local/bin /usr/local/bin

# 소스 코드 복사 (ai 폴더 내부의 모든 파일)
COPY . .

# 환경 변수 설정 (Python 출력 버퍼링 비활성화 등)
ENV PYTHONUNBUFFERED=1
ENV PYTHONDONTWRITEBYTECODE=1

# FastAPI 서버 실행 -> docker-compose.yml context ./ai로 설정 필요

EXPOSE 8000

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]