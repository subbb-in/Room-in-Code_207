"""
임베딩 서비스 - Sentence Transformers를 사용한 텍스트 임베딩
"""
import logging
from typing import List, Union
from sentence_transformers import SentenceTransformer
from app.core.config import settings

logger = logging.getLogger(__name__)

# 모델 싱글톤 (lazy loading)
_model: SentenceTransformer = None


def get_embedding_model() -> SentenceTransformer:
    """임베딩 모델 싱글톤 반환"""
    global _model
    if _model is None:
        logger.info(f"Loading embedding model: {settings.EMBEDDING_MODEL}")
        _model = SentenceTransformer(settings.EMBEDDING_MODEL)
    return _model


def embed_text(text: str) -> List[float]:
    """단일 텍스트를 임베딩 벡터로 변환"""
    model = get_embedding_model()
    embedding = model.encode(text, convert_to_numpy=True)
    return embedding.tolist()


def embed_texts(texts: List[str]) -> List[List[float]]:
    """여러 텍스트를 임베딩 벡터 리스트로 변환"""
    model = get_embedding_model()
    embeddings = model.encode(texts, convert_to_numpy=True, show_progress_bar=True)
    return embeddings.tolist()


def get_embedding_dimension() -> int:
    """임베딩 벡터 차원 반환 (all-MiniLM-L6-v2: 384)"""
    model = get_embedding_model()
    return model.get_sentence_embedding_dimension()
