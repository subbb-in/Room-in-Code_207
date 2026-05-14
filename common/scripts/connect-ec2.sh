#!/bin/bash

# --- 설정 변수 ---
DEFAULT_PEM_PATH="/c/Users/SSAFY/S14P11A208/common/key/I14A208T.pem"
DOMAIN="i14A208.p.ssafy.io"
USER="ubuntu"

echo "--------------------------------------------------"
echo "🌐 SSAFY EC2 접속 매니저 ($DOMAIN)"
echo "--------------------------------------------------"

# 1. PEM 키 경로 입력 받기
echo "🔑 PEM 파일 경로를 입력하세요."
echo "   (아무것도 입력하지 않으면 기본 경로가 사용됩니다: $DEFAULT_PEM_PATH)"
read -p "👉 경로 입력: " INPUT_PATH

# 입력값이 없으면 기본값 사용
PEM_PATH=${INPUT_PATH:-$DEFAULT_PEM_PATH}

# 2. 파일 존재 여부 확인
if [ ! -f "$PEM_PATH" ]; then
    echo "❌ 에러: 해당 경로에 PEM 파일이 존재하지 않습니다: $PEM_PATH"
    exit 1
fi

echo "🚀 Connecting to SSAFY EC2..."
echo "📍 Identity File: $PEM_PATH"

# 3. SSH 접속
ssh -i "$PEM_PATH" "$USER@$DOMAIN"