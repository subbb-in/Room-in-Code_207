"""
Qdrant 서비스 - 벡터 저장 및 하이브리드 검색
"""
import logging
import json
from typing import List, Dict, Any, Optional
from qdrant_client import models
from qdrant_client.models import (
    Distance, 
    VectorParams,
    PointStruct,
    NamedVector,
    SearchRequest,
    Filter,
)
from app.core.qdrant_client import qdrant_client
from app.core.config import settings
from app.services.embedding_service import embed_text, embed_texts, get_embedding_dimension

logger = logging.getLogger(__name__)

# 가중치 설정
QUESTION_WEIGHT = 0.7
SOLUTION_WEIGHT = 0.3


def init_collection() -> bool:
    """
    Qdrant 컬렉션 초기화 (Named Vectors 사용)
    - question_vector: 질문 임베딩 (70% 가중치)
    - solution_vector: 솔루션 임베딩 (30% 가중치)
    """
    collection_name = settings.QDRANT_COLLECTION_NAME
    
    try:
        # 컬렉션 존재 여부 확인
        collections = qdrant_client.get_collections()
        existing_names = [c.name for c in collections.collections]
        
        if collection_name in existing_names:
            logger.info(f"Collection '{collection_name}' already exists")
            return True
        
        # 임베딩 차원 가져오기
        vector_size = get_embedding_dimension()
        
        # Named Vectors로 컬렉션 생성
        qdrant_client.create_collection(
            collection_name=collection_name,
            vectors_config={
                "question_vector": VectorParams(
                    size=vector_size,
                    distance=Distance.COSINE
                ),
                "solution_vector": VectorParams(
                    size=vector_size,
                    distance=Distance.COSINE
                )
            }
        )
        logger.info(f"Created collection '{collection_name}' with vector size {vector_size}")
        return True
        
    except Exception as e:
        logger.error(f"Failed to initialize collection: {e}")
        return False


def upsert_problems(problems: List[Dict[str, Any]]) -> bool:
    """
    문제 데이터를 Qdrant에 저장
    
    Args:
        problems: 문제 리스트, 각 문제는 다음 키 포함:
            - id: 문제 ID
            - question: 문제 텍스트
            - solutions: 솔루션 텍스트 (JSON 문자열 또는 리스트)
            - difficulty: 난이도
            - url: 문제 URL
    """
    collection_name = settings.QDRANT_COLLECTION_NAME
    
    try:
        # 텍스트 추출
        questions = [p["question"] for p in problems]
        
        # solutions 처리 (JSON 문자열이면 파싱 후 첫 번째 솔루션 사용)
        solutions_texts = []
        for p in problems:
            sol = p.get("solutions", "")
            if isinstance(sol, str):
                try:
                    sol_list = json.loads(sol)
                    sol = sol_list[0] if sol_list else ""
                except json.JSONDecodeError:
                    pass
            elif isinstance(sol, list):
                sol = sol[0] if sol else ""
            solutions_texts.append(str(sol)[:2000])  # 길이 제한
        
        # 배치 임베딩
        logger.info(f"Embedding {len(questions)} questions...")
        question_embeddings = embed_texts(questions)
        
        logger.info(f"Embedding {len(solutions_texts)} solutions...")
        solution_embeddings = embed_texts(solutions_texts)
        
        # PointStruct 생성
        points = []
        for i, p in enumerate(problems):
            point = PointStruct(
                id=p["id"],
                vector={
                    "question_vector": question_embeddings[i],
                    "solution_vector": solution_embeddings[i]
                },
                payload={
                    "question": p["question"][:500],  # 저장 시 길이 제한
                    "difficulty": p.get("difficulty", "unknown"),
                    "url": p.get("url", ""),
                    "name": p.get("name", "")
                }
            )
            points.append(point)
        
        # 배치 upsert
        batch_size = 100
        for i in range(0, len(points), batch_size):
            batch = points[i:i+batch_size]
            qdrant_client.upsert(
                collection_name=collection_name,
                points=batch
            )
            logger.info(f"Upserted batch {i//batch_size + 1}/{(len(points)-1)//batch_size + 1}")
        
        logger.info(f"Successfully upserted {len(points)} problems")
        return True
        
    except Exception as e:
        logger.error(f"Failed to upsert problems: {e}")
        return False


def hybrid_search(query_question: str, top_k: int = 2) -> List[Dict[str, Any]]:
    """
    하이브리드 검색: Question(70%) + Solution(30%) 가중치 적용
    
    Args:
        query_question: 검색할 문제 텍스트
        top_k: 반환할 결과 수
        
    Returns:
        유사한 문제 리스트 (url, difficulty, score 포함)
    """
    collection_name = settings.QDRANT_COLLECTION_NAME
    
    try:
        # 쿼리 임베딩
        query_embedding = embed_text(query_question)
        
        # 두 벡터에 대해 각각 검색 (qdrant-client 1.16.x API)
        question_results = qdrant_client.query_points(
            collection_name=collection_name,
            query=query_embedding,
            using="question_vector",
            limit=top_k * 3  # 더 많이 가져와서 가중 합산
        ).points
        
        solution_results = qdrant_client.query_points(
            collection_name=collection_name,
            query=query_embedding,
            using="solution_vector",
            limit=top_k * 3
        ).points
        
        # 점수 합산 (가중치 적용)
        scores: Dict[int, float] = {}
        payloads: Dict[int, Dict] = {}
        
        for result in question_results:
            point_id = result.id
            scores[point_id] = scores.get(point_id, 0) + result.score * QUESTION_WEIGHT
            payloads[point_id] = result.payload
        
        for result in solution_results:
            point_id = result.id
            scores[point_id] = scores.get(point_id, 0) + result.score * SOLUTION_WEIGHT
            if point_id not in payloads:
                payloads[point_id] = result.payload
        
        # 점수순 정렬
        sorted_ids = sorted(scores.keys(), key=lambda x: scores[x], reverse=True)[:top_k]
        
        # 결과 구성
        results = []
        for point_id in sorted_ids:
            payload = payloads[point_id]
            results.append({
                "id": point_id,
                "url": payload.get("url", ""),
                "difficulty": payload.get("difficulty", "unknown"),
                "name": payload.get("name", ""),
                "score": round(scores[point_id], 4)
            })
        
        return results
        
    except Exception as e:
        logger.error(f"Hybrid search failed: {e}")
        return []


def get_collection_info() -> Optional[Dict[str, Any]]:
    """컬렉션 정보 조회"""
    try:
        info = qdrant_client.get_collection(settings.QDRANT_COLLECTION_NAME)
        return {
            "name": settings.QDRANT_COLLECTION_NAME,
            "points_count": info.points_count,
            "status": str(info.status)
        }
    except Exception as e:
        logger.error(f"Failed to get collection info: {e}")
        return None
