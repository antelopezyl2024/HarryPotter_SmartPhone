"""SQLAlchemy ORM models for player progress and audit logs.

The DB layer is the only place game state is mutated. AI Service and RAG
Service never touch these models directly.
"""

from __future__ import annotations

from datetime import datetime
from typing import Any, Optional

from sqlalchemy import JSON, Boolean, DateTime, ForeignKey, Integer, String
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column


class Base(DeclarativeBase):
    pass


class Player(Base):
    __tablename__ = "players"

    player_uuid: Mapped[str] = mapped_column(String, primary_key=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)
    last_active_at: Mapped[datetime] = mapped_column(
        DateTime, default=datetime.utcnow, onupdate=datetime.utcnow
    )


class Playthrough(Base):
    __tablename__ = "playthroughs"

    playthrough_id: Mapped[str] = mapped_column(String, primary_key=True)
    player_uuid: Mapped[str] = mapped_column(
        String, ForeignKey("players.player_uuid"), index=True
    )
    dlc_id: Mapped[str] = mapped_column(String, index=True)
    current_scene_id: Mapped[Optional[str]] = mapped_column(String, nullable=True)
    total_score: Mapped[int] = mapped_column(Integer, default=0)
    status: Mapped[str] = mapped_column(String, default="IN_PROGRESS")
    started_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)
    ended_at: Mapped[Optional[datetime]] = mapped_column(DateTime, nullable=True)
    ending_id: Mapped[Optional[str]] = mapped_column(String, nullable=True)


class DecisionLog(Base):
    __tablename__ = "decision_logs"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    playthrough_id: Mapped[str] = mapped_column(
        String, ForeignKey("playthroughs.playthrough_id"), index=True
    )
    scene_id: Mapped[str] = mapped_column(String)
    mode_used: Mapped[str] = mapped_column(String)  # CHOICE | FREE_TEXT
    payload: Mapped[dict[str, Any]] = mapped_column(JSON)
    ai_classification: Mapped[Optional[dict[str, Any]]] = mapped_column(JSON, nullable=True)
    is_correct: Mapped[bool] = mapped_column(Boolean)
    delta_score: Mapped[int] = mapped_column(Integer)
    narrative_feedback: Mapped[str] = mapped_column(String)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)


class HermioneQuery(Base):
    __tablename__ = "hermione_queries"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    player_uuid: Mapped[str] = mapped_column(String, index=True)
    playthrough_id: Mapped[Optional[str]] = mapped_column(String, nullable=True)
    query: Mapped[str] = mapped_column(String)
    answer: Mapped[str] = mapped_column(String)
    retrieved_lore_ids: Mapped[list[str]] = mapped_column(JSON)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)
