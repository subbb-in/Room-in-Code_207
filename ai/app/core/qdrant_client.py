"""
Qdrant 클라이언트 싱글톤 인스턴스
"""
from qdrant_client import QdrantClient
from app.core.config import settings

# Qdrant 클라이언트 싱글톤
qdrant_client = QdrantClient(
    host=settings.QDRANT_HOST,
    port=settings.QDRANT_PORT
)
