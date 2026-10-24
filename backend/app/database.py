import os
from sqlalchemy import create_engine
from .config import validate_database_configuration
from sqlalchemy.orm import DeclarativeBase, sessionmaker

validate_database_configuration(os.environ)
DATABASE_URL = os.getenv("DATABASE_URL", "sqlite:///./stayeasy_dev.db")
connect_args = {"check_same_thread": False} if DATABASE_URL.startswith("sqlite") else {}
engine = create_engine(DATABASE_URL, pool_pre_ping=True, connect_args=connect_args)
SessionLocal = sessionmaker(bind=engine, autoflush=False, expire_on_commit=False)

class Base(DeclarativeBase):
    pass

def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
