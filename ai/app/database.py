from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from .models import Base
from app.core.config import settings  # 설정 임포트

# MySQL 설정
SQLALCHEMY_DATABASE_URL = settings.DATABASE_URL
engine = create_engine(SQLALCHEMY_DATABASE_URL,pool_recycle=3600,pool_pre_ping=True)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)


def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

def init_db():
    Base.metadata.create_all(bind=engine)
