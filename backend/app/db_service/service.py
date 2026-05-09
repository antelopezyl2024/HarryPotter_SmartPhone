"""DB Service repository functions. Game Logic calls these; AI/RAG never do."""

from __future__ import annotations

import uuid
from datetime import datetime
from typing import Any, Optional

from sqlalchemy.orm import Session

from app.db_service.models import DecisionLog, HermioneQuery, Player, Playthrough


# ---------- Player ----------

def get_or_create_player(session: Session, player_uuid: str) -> Player:
    player = session.get(Player, player_uuid)
    if player is None:
        player = Player(player_uuid=player_uuid)
        session.add(player)
        session.flush()
    else:
        player.last_active_at = datetime.utcnow()
    return player


# ---------- Playthrough ----------

def get_in_progress_playthrough(
    session: Session, player_uuid: str, dlc_id: str
) -> Optional[Playthrough]:
    return (
        session.query(Playthrough)
        .filter_by(player_uuid=player_uuid, dlc_id=dlc_id, status="IN_PROGRESS")
        .order_by(Playthrough.started_at.desc())
        .first()
    )


def get_playthrough(session: Session, playthrough_id: str) -> Optional[Playthrough]:
    return session.get(Playthrough, playthrough_id)


def create_playthrough(
    session: Session, player_uuid: str, dlc_id: str, first_scene_id: str
) -> Playthrough:
    p = Playthrough(
        playthrough_id=str(uuid.uuid4()),
        player_uuid=player_uuid,
        dlc_id=dlc_id,
        current_scene_id=first_scene_id,
        total_score=0,
        status="IN_PROGRESS",
    )
    session.add(p)
    session.flush()
    return p


# ---------- DecisionLog ----------

def write_decision_log(session: Session, **kwargs: Any) -> DecisionLog:
    dl = DecisionLog(**kwargs)
    session.add(dl)
    session.flush()
    return dl


def list_decision_logs(session: Session, playthrough_id: str) -> list[DecisionLog]:
    return (
        session.query(DecisionLog)
        .filter_by(playthrough_id=playthrough_id)
        .order_by(DecisionLog.id)
        .all()
    )


# ---------- HermioneQuery ----------

def write_hermione_query(session: Session, **kwargs: Any) -> HermioneQuery:
    hq = HermioneQuery(**kwargs)
    session.add(hq)
    session.flush()
    return hq
