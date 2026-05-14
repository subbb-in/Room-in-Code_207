from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from contextlib import asynccontextmanager
from app.api.router import api_router
from app.database import init_db
from app.services.qdrant_service import get_collection_info

# from app.database import init_chroma


# 1. Lifespan 정의: 서버 시작 시 DB 초기화 수행
@asynccontextmanager
async def lifespan(app: FastAPI):
    # 서버 시작 시 MySQL, Qdrant 초기화
    init_db()
    # init_chroma()
    
    # Qdrant 연결 확인
    try:
        info = get_collection_info()
        if info:
            print(f"[Qdrant] Collection '{info['name']}' ready - {info['points_count']} points")
        else:
            print("[Qdrant] Collection not found. Run 'python -m scripts.ingest_taco_data' to create.")
    except Exception as e:
        print(f"[Qdrant] Connection check failed: {e}")
    
    yield
    # 서버 종료 시 실행될 로직 (필요 시 작성)

# 2. FastAPI 인스턴스 생성 (lifespan 핸들러 연결)
app = FastAPI(
    title="SSAFY Hamcheese AI Server",
    description="AI Feedback 및 문제 추천 API 서버",
    version="1.0.0",
    lifespan=lifespan
)

# 3. CORS 설정
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 4. 라우터 연결
app.include_router(api_router)

@app.get("/")
def health_check():
    return {"status": "healthy", "service": "SSAFY Hamcheese AI Server"}