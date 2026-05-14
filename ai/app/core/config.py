from pydantic_settings import BaseSettings, SettingsConfigDict
from pathlib import Path

# ai 폴더 > > > .env
_ROOT_DIR = Path(__file__).resolve().parent.parent.parent
_ENV_FILE = _ROOT_DIR / ".env"

# 컴파일 시 인자로 전달된 값 > 시스템 환경 변수 > .env
class Settings(BaseSettings):

    # DB 설정
    DB_USER: str
    DB_PASSWORD: str
    DB_HOST: str
    DB_PORT: str
    DB_NAME: str

    # API 설정
    ANTHROPIC_API_KEY: str
    ANTHROPIC_BASE_URL: str

    # Qdrant 설정
    QDRANT_HOST: str
    QDRANT_PORT: int
    QDRANT_COLLECTION_NAME: str = "taco_problems"
    
    # Embedding 설정
    EMBEDDING_MODEL: str = "sentence-transformers/all-MiniLM-L6-v2"
    
    # HuggingFace 설정
    HF_TOKEN: str

    # # Chroma DB 설정
    # CHROMA_DB_NAME : str
    # ALGO_COLLECTION_NAME : str
    # PDF_NAME :str
    #
    # CHUNK_SIZE : int = 1000
    # CHUNK_OVERLAP : int = 100

# V1 내부 Config 클래스 정의
    # class Config:
    #     env_file = ".env" if Path(".env").exists() else None
    # V2 model_config 속성에 ConfigDict 인스턴스를 할당하기
    model_config = SettingsConfigDict(
        env_file=_ENV_FILE,
        env_file_encoding='utf-8',
        extra='ignore'
    )
    @property
    def DATABASE_URL(self) -> str:
        return f"mysql+pymysql://{self.DB_USER}:{self.DB_PASSWORD}@{self.DB_HOST}:{self.DB_PORT}/{self.DB_NAME}"

    # @property
    # def CHROMA_DB_PATH(self) -> str:
    #     """루트 기준 chroma_db의 절대 경로 반환"""
    #     return str(_ROOT_DIR / self.CHROMA_DB_NAME)

    # @property
    # def PDF_PATH(self) -> str:
    #     """루트/data 폴더 내의 PDF 파일 절대 경로 반환"""
    #     return str(_ROOT_DIR / "data" / self.PDF_NAME)

settings = Settings()
