"""
TACO 데이터셋 적재 스크립트
Usage: python -m scripts.ingest_taco_data
"""
import sys
import logging
from pathlib import Path

# 프로젝트 루트를 path에 추가
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

import datasets
from huggingface_hub import hf_hub_download
from app.core.config import settings
from app.services.qdrant_service import init_collection, upsert_problems

# 로깅 설정
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# 설정
SAMPLE_SIZE = 3000
REPO_ID = "BAAI/TACO"

# Arrow 파일 경로 (TACO.py에서 참조)
TRAIN_FILES = [
    'train/data-00000-of-00009.arrow',
    'train/data-00001-of-00009.arrow',
    'train/data-00002-of-00009.arrow',
    'train/data-00003-of-00009.arrow',
    'train/data-00004-of-00009.arrow',
    'train/data-00005-of-00009.arrow',
    'train/data-00006-of-00009.arrow',
    'train/data-00007-of-00009.arrow',
    'train/data-00008-of-00009.arrow',
]


def download_and_load_dataset():
    """Arrow 파일을 직접 다운로드하여 Dataset으로 로드"""
    logger.info("Downloading TACO Arrow files from HuggingFace...")
    
    local_files = []
    hf_token = settings.HF_TOKEN if settings.HF_TOKEN else None
    
    for file_path in TRAIN_FILES:
        logger.info(f"Downloading {file_path}...")
        local_path = hf_hub_download(
            repo_id=REPO_ID,
            filename=file_path,
            repo_type="dataset",
            token=hf_token
        )
        local_files.append(local_path)
    
    logger.info("Loading Arrow files into Dataset...")
    dataset_list = [datasets.Dataset.from_file(f) for f in local_files]
    dataset = datasets.concatenate_datasets(dataset_list)
    
    return dataset


def main():
    logger.info("Starting TACO dataset ingestion...")
    
    # 1. 데이터셋 로드
    dataset = download_and_load_dataset()
    logger.info(f"Loaded {len(dataset)} rows from train split")
    
    # 2. 샘플링 (3000개)
    if len(dataset) > SAMPLE_SIZE:
        dataset = dataset.shuffle(seed=42).select(range(SAMPLE_SIZE))
        logger.info(f"Sampled {SAMPLE_SIZE} rows")
    
    # 3. 컬렉션 초기화
    logger.info("Initializing Qdrant collection...")
    if not init_collection():
        logger.error("Failed to initialize collection")
        return
    
    # 4. 데이터 변환
    logger.info("Preparing data for ingestion...")
    problems = []
    for idx, row in enumerate(dataset):
        problem = {
            "id": idx,
            "question": row.get("question", ""),
            "solutions": row.get("solutions", ""),
            "difficulty": row.get("difficulty", "unknown"),
            "url": row.get("url", ""),
            "name": row.get("name", "")
        }
        problems.append(problem)
    
    # 5. Qdrant에 저장
    logger.info(f"Upserting {len(problems)} problems to Qdrant...")
    if upsert_problems(problems):
        logger.info("Data ingestion completed successfully!")
    else:
        logger.error("Data ingestion failed")


if __name__ == "__main__":
    main()
