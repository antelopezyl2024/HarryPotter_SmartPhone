"""Game Logic Service: the only writer of game state.

Owns scene transitions, decision validation, scoring, and ending resolution.
AI output (free-text classification) flows in as a structured value and is
applied via deterministic lookups in `scoring.py` — never directly used to
mutate state.
"""

from __future__ import annotations

from datetime import datetime
from typing import Any

from sqlalchemy.orm import Session

from app.ai_service import service as ai_svc
from app.api.schemas import (
    AIClassification,
    ChoiceDecisionRequest,
    EndingId,
    FreeTextDecisionRequest,
    Playthrough as PlaythroughSchema,
    PlaythroughStatus,
    Scene,
    SubmitDecisionResponse,
)
from app.db_service import service as db_svc
from app.db_service.models import Playthrough as PlaythroughDB
from app.game_logic.content_loader import DLCContent
from app.game_logic.scoring import is_category_correct, is_choice_correct


def to_schema(p: PlaythroughDB) -> PlaythroughSchema:
    return PlaythroughSchema(
        playthrough_id=p.playthrough_id,
        player_uuid=p.player_uuid,
        dlc_id=p.dlc_id,
        current_scene_id=p.current_scene_id,
        total_score=p.total_score,
        status=PlaythroughStatus(p.status),
        started_at=p.started_at,
        ended_at=p.ended_at,
        ending_id=EndingId(p.ending_id) if p.ending_id else None,
    )


def start_or_resume(
    session: Session, dlc: DLCContent, player_uuid: str
) -> tuple[PlaythroughDB, Scene]:
    db_svc.get_or_create_player(session, player_uuid)
    existing = db_svc.get_in_progress_playthrough(session, player_uuid, dlc.dlc_id)
    if existing is not None:
        return existing, dlc.get_scene(existing.current_scene_id)
    p = db_svc.create_playthrough(session, player_uuid, dlc.dlc_id, dlc.scene_order[0])
    return p, dlc.first_scene()


def apply_decision(
    session: Session,
    dlc: DLCContent,
    p: PlaythroughDB,
    request: ChoiceDecisionRequest | FreeTextDecisionRequest,
) -> SubmitDecisionResponse:
    """Validate, score, persist, advance state, and generate narrative feedback."""
    if p.status != "IN_PROGRESS":
        raise ValueError("Playthrough has already been completed.")
    if request.scene_id != p.current_scene_id:
        raise ValueError(
            f"scene_id {request.scene_id!r} does not match current scene "
            f"{p.current_scene_id!r}."
        )

    scene = dlc.get_scene(p.current_scene_id)
    scene_dict = dlc.get_scene_definition(p.current_scene_id)

    ai_class: AIClassification | None = None

    if isinstance(request, ChoiceDecisionRequest):
        is_correct, hint = is_choice_correct(scene_dict, request.choice_id)
        action_summary = f"Chose option {request.choice_id}"
        payload: dict[str, Any] = {"choice_id": request.choice_id}
        mode_used = "CHOICE"
    elif isinstance(request, FreeTextDecisionRequest):
        ft = scene_dict["decision"]["free_text"]
        cls = ai_svc.classify_free_text(
            scene_title=scene.title,
            scene_narrative=scene.narrative,
            prompt=ft["prompt"],
            criteria=ft["llm_classification_criteria"],
            categories=ft["categories"],
            player_text=request.text,
        )
        is_correct, label = is_category_correct(scene_dict, cls["category_id"])
        hint = label
        action_summary = (
            f'Free-text input: "{request.text}" classified as {cls["category_id"]}'
        )
        ai_class = AIClassification(
            category_id=cls["category_id"], confidence=cls["confidence"]
        )
        payload = {"text": request.text}
        mode_used = "FREE_TEXT"
    else:
        raise ValueError(f"Unsupported decision type {type(request).__name__}.")

    delta = 1 if is_correct else 0

    feedback = ai_svc.generate_narrative_feedback(
        scene_title=scene.title,
        scene_narrative=scene.narrative,
        action_summary=action_summary,
        is_correct=is_correct,
        hint=hint,
    )

    db_svc.write_decision_log(
        session,
        playthrough_id=p.playthrough_id,
        scene_id=p.current_scene_id,
        mode_used=mode_used,
        payload=payload,
        ai_classification=ai_class.model_dump() if ai_class else None,
        is_correct=is_correct,
        delta_score=delta,
        narrative_feedback=feedback,
    )

    # Advance state
    p.total_score += delta
    next_id = scene.next_scene_id
    if next_id:
        p.current_scene_id = next_id
        next_scene: Scene | None = dlc.get_scene(next_id)
        status = PlaythroughStatus.IN_PROGRESS
    else:
        ending = dlc.resolve_ending(p.total_score)
        p.current_scene_id = None
        p.ended_at = datetime.utcnow()
        p.ending_id = ending["id"]
        p.status = "COMPLETED"
        next_scene = None
        status = PlaythroughStatus.COMPLETED

    return SubmitDecisionResponse(
        is_correct=is_correct,
        delta_score=delta,
        total_score=p.total_score,
        narrative_feedback=feedback,
        ai_classification=ai_class,
        next_scene=next_scene,
        playthrough_status=status,
    )


def resolve_ending(
    session: Session, dlc: DLCContent, p: PlaythroughDB
) -> dict[str, Any]:
    if p.status != "COMPLETED" or p.ending_id is None:
        raise ValueError("Playthrough has not yet reached an ending.")

    ending = next(e for e in dlc.endings if e["id"] == p.ending_id)
    decisions = db_svc.list_decision_logs(session, p.playthrough_id)

    decision_dicts: list[dict[str, Any]] = []
    for d in decisions:
        if d.mode_used == "CHOICE":
            summary = f"Picked {d.payload['choice_id']}"
        else:
            summary = f'Said: "{d.payload["text"]}"'
        decision_dicts.append(
            {
                "scene_id": d.scene_id,
                "mode_used": d.mode_used,
                "is_correct": d.is_correct,
                "summary": summary,
            }
        )

    ai_summary = ai_svc.generate_ending_summary(
        decisions=decision_dicts,
        ending_id=p.ending_id,
        total_score=p.total_score,
    )

    return {
        "ending_id": p.ending_id,
        "title": ending["title"],
        "narrative": ending["narrative"],
        "summary": ai_summary,
        "total_score": p.total_score,
        "decisions_overview": decision_dicts,
    }
