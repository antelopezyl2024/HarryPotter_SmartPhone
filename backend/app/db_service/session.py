"""SQLite engine + session factory. Initialized once at app startup."""

from __future__ import annotations

from sqlalchemy import create_engine
from sqlalchemy.orm import Session, sessionmaker

from app.config import settings
from app.db_service.models import Base

_engine = None
_SessionFactory: sessionmaker | None = None


def init_db() -> None:
    """Create the SQLite file (if missing) and all tables."""
    global _engine, _SessionFactory
    settings.sqlite_path.parent.mkdir(parents=True, exist_ok=True)
    _engine = create_engine(
        f"sqlite:///{settings.sqlite_path}",
        echo=False,
        connect_args={"check_same_thread": False},
    )
    Base.metadata.create_all(_engine)
    _SessionFactory = sessionmaker(bind=_engine, expire_on_commit=False)


def get_session() -> Session:
    if _SessionFactory is None:
        raise RuntimeError("DB not initialized — call init_db() at startup.")
    return _SessionFactory()
