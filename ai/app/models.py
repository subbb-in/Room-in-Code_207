import enum
from sqlalchemy import Column, Integer, BigInteger, String, Text, DateTime, Enum, Boolean, ForeignKey, JSON, \
    UniqueConstraint
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func
from sqlalchemy.ext.declarative import declarative_base

Base = declarative_base()


# --- Enum 정의 ---
class LoginType(enum.Enum):
    SOCIAL = "SOCIAL"
    LOGIN = "LOGIN"


class Difficulty(enum.Enum):
    EASY = "EASY"
    MEDIUM = "MEDIUM"
    MEDIUM_HARD = "MEDIUM_HARD"
    HARD = "HARD"
    VERY_HARD = "VERY_HARD"


# --- 다대다(Many-to-Many) 연결 테이블 ---
class ProblemSkill(Base):
    __tablename__ = "problem_skills"

    problem_id = Column(BigInteger, ForeignKey("problems.problem_id", ondelete="CASCADE"), primary_key=True)
    skill_id = Column(BigInteger, ForeignKey("skills.skill_id", ondelete="CASCADE"), primary_key=True)


# --- 테이블 클래스 ---

class User(Base):
    __tablename__ = "users"

    user_id = Column(BigInteger, primary_key=True, autoincrement=True, comment="PK: 자동 증가")
    joined_at = Column(DateTime, nullable=False, server_default=func.now(), comment="가입 시점")
    updated_at = Column(DateTime, onupdate=func.now(), comment="수정 시점")
    login_type = Column(Enum(LoginType), nullable=False, default=LoginType.LOGIN, comment="소셜 로그인 or 로컬 로그인")
    is_deleted = Column(Boolean, default=False)

    # Relationships
    profile = relationship("UserProfileInfo", back_populates="user", uselist=False)
    social_account = relationship("Social", back_populates="user", uselist=False)
    local_account = relationship("Local", back_populates="user", uselist=False)
    histories = relationship("History", back_populates="user")
    skill_stats = relationship("UserSkillStatistic", back_populates="user")


class UserProfileInfo(Base):
    __tablename__ = "user_profile_info"

    profile_id = Column(BigInteger, primary_key=True, autoincrement=True)
    user_id = Column(BigInteger, ForeignKey("users.user_id", ondelete="CASCADE"), unique=True, nullable=False)
    nickname = Column(String(50), nullable=False)
    profile_image_url = Column(String(255))
    intro_text = Column(String(255))
    total_xp = Column(Integer, nullable=False, default=0, comment="누적 xp")

    user = relationship("User", back_populates="profile")


class Social(Base):
    __tablename__ = "social"
    __table_args__ = (UniqueConstraint('provider_id', 'oauth_id', name='UK_OAUTH_ACCOUNT'),)

    social_id = Column(BigInteger, primary_key=True, autoincrement=True)
    provider_id = Column(String(255), nullable=False, comment="Google, GitHub, Naver")
    oauth_id = Column(String(255), nullable=False)
    user_id = Column(BigInteger, ForeignKey("users.user_id", ondelete="CASCADE"), nullable=False)

    user = relationship("User", back_populates="social_account")


class Local(Base):
    __tablename__ = "local"

    login_id = Column(BigInteger, primary_key=True, autoincrement=True)
    email = Column(String(100), nullable=False, unique=True)
    password = Column(String(255), nullable=False, comment="Hash값")
    user_id = Column(BigInteger, ForeignKey("users.user_id", ondelete="CASCADE"), nullable=False)

    user = relationship("User", back_populates="local_account")


class Skill(Base):
    __tablename__ = "skills"

    skill_id = Column(BigInteger, primary_key=True, autoincrement=True)
    skill_name = Column(String(100), nullable=False, comment="8개 속성 (BFS, DP 등)")

    problems = relationship("Problem", secondary="problem_skills", back_populates="skills")
    user_stats = relationship("UserSkillStatistic", back_populates="skill")


class Problem(Base):
    __tablename__ = "problems"

    problem_id = Column(BigInteger, primary_key=True, autoincrement=True)
    question = Column(Text, nullable=False)
    solutions = Column(Text, comment="정답 코드")
    difficulty = Column(Enum(Difficulty), nullable=False, default=Difficulty.EASY)
    source = Column(String(255), comment="코드 출처")
    url = Column(String(500))
    time_limit = Column(String(50))
    memory_limit = Column(String(50))
    input_output = Column(JSON)
    expected_aux_space = Column(String(100))
    expected_time_complexity = Column(String(100))

    hints = relationship("ProblemHint", back_populates="problem", cascade="all, delete-orphan")
    skills = relationship("Skill", secondary="problem_skills", back_populates="problems")
    histories = relationship("History", back_populates="problem")


class ProblemHint(Base):
    __tablename__ = "problem_hints"

    hint_id = Column(BigInteger, primary_key=True, autoincrement=True)
    sequence = Column(Integer, nullable=False, comment="힌트 순서")
    content = Column(Text, nullable=False, comment="힌트 내용")
    problem_id = Column(BigInteger, ForeignKey("problems.problem_id", ondelete="CASCADE"), nullable=False)

    problem = relationship("Problem", back_populates="hints")


class UserSkillStatistic(Base):
    __tablename__ = "user_skill_statistics"

    user_id = Column(BigInteger, ForeignKey("users.user_id", ondelete="CASCADE"), primary_key=True)
    skill_id = Column(BigInteger, ForeignKey("skills.skill_id", ondelete="CASCADE"), primary_key=True)
    solved_count = Column(Integer, nullable=False, default=0, comment="해당 스킬 문제 풀이 횟수")

    user = relationship("User", back_populates="skill_stats")
    skill = relationship("Skill", back_populates="user_stats")


class History(Base):
    __tablename__ = "history"

    history_id = Column(BigInteger, primary_key=True, autoincrement=True)
    problem_id = Column(BigInteger, ForeignKey("problems.problem_id"), nullable=False)
    user_id = Column(BigInteger, ForeignKey("users.user_id", ondelete="CASCADE"), nullable=False)
    created_at = Column(DateTime, nullable=False, server_default=func.now())

    user = relationship("User", back_populates="histories")
    problem = relationship("Problem", back_populates="histories")
    report = relationship("AIReport", back_populates="history", uselist=False)


class AIReport(Base):
    __tablename__ = "ai_reports"

    report_id = Column(BigInteger, primary_key=True, autoincrement=True)
    game_result = Column(JSON, comment="게임 결과 데이터")
    graded_result = Column(JSON, comment="채점 결과")
    ai_feedback = Column(JSON, comment="AI 피드백 데이터")
    created_at = Column(DateTime, nullable=False, server_default=func.now())
    problem_title = Column(String(255), nullable=False)
    history_id = Column(BigInteger, ForeignKey("history.history_id", ondelete="CASCADE"), nullable=False)

    history = relationship("History", back_populates="report")