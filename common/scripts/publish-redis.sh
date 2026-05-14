#!/bin/bash

set +e

echo "--------------------------------------------------"
echo "--------------------------------------------------"

# 에러 발생 시 10초 대기 후 종료하는 함수
error_exit() {
    echo "--------------------------------------------------"
    echo "❌ 에러 발생: $1"
    echo "⚠️  메시지 확인을 위해 10초 후 창이 닫힙니다..."
    echo "--------------------------------------------------"
    sleep 10
    exit 1
}

# 1. 입력 받기
read -p "👤 1. Docker Hub ID: " DOCKER_ID
read -p "🏷️  2. 이미지 TAG (기본 1.0): " TAG

echo "📋 현재 실행 중인 컨테이너 목록:"
docker ps --format "table {{.Names}}\t{{.Image}}"
echo "--------------------------------------------------"

read -p "🐳 3. 데이터를 추출할 컨테이너 이름: " SOURCE_CONTAINER

if [ -z "$DOCKER_ID" ] || [ -z "$SOURCE_CONTAINER" ]; then
    error_exit "Docker ID와 컨테이너 이름은 필수 입력 사항입니다."
fi


if ! mkdir -p "$WORK_DIR"; then
fi

cd "$WORK_DIR" || error_exit "해당 경로로 이동할 수 없습니다."

# 3. 데이터 추출
echo "📦 컨테이너($SOURCE_CONTAINER) 데이터 저장(SAVE) 중..."
if ! docker exec "$SOURCE_CONTAINER" redis-cli save; then
fi

echo "🔍 dump.rdb 파일 가져오는 중..."
fi

# 4. 빌드 및 푸시
IMAGE_NAME="hamcheese-redis-dev"
FULL_IMAGE_NAME="${DOCKER_ID}/${IMAGE_NAME}:${TAG}"

echo "📝 Dockerfile 생성 및 빌드 중..."
FROM redis:latest
COPY dump.rdb /data/dump.rdb
USER root
RUN chown redis:redis /data/dump.rdb
USER redis
EOF

fi

echo "📤 Docker Hub 푸시 중..."
if ! docker push "$FULL_IMAGE_NAME"; then
fi

# 5. 정리 및 마무리

echo "--------------------------------------------------"
echo "🎉 모든 작업이 완료되었습니다!"
echo "웹에서 확인: https://hub.docker.com/r/${DOCKER_ID}/${IMAGE_NAME}"
echo "--------------------------------------------------"
read -p "엔터를 누르면 창이 닫힙니다..."