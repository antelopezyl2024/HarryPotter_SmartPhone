"""API Gateway — all REST endpoints.

Module-level `_DLCS` is populated at startup (see `app.main.lifespan`) so
handlers can resolve content without re-reading disk per request.
"""

from __future__ import annotations

from fastapi import APIRouter, Depends, HTTPException, Path
from sqlalchemy.orm import Session

from app.api.schemas import (
    DLCSummary,
    GetEndingResponse,
    GetSceneResponse,
    HermioneQueryRequest,
    HermioneQueryResponse,
    ListDLCsResponse,
    StartPlaythroughRequest,
    StartPlaythroughResponse,
    SubmitDecisionRequest,
    SubmitDecisionResponse,
)
from app.db_service import service as db_svc
from app.db_service.session import get_session
from app.game_logic import service as game_logic
from app.game_logic.content_loader import DLCContent
from app.hermione import service as hermione

router = APIRouter(prefix="/api/v1")

_DLCS: dict[str, DLCContent] = {}


def set_dlcs(dlcs: dict[str, DLCContent]) -> None:
    """Called at startup by `app.main.lifespan` to inject loaded content."""
    global _DLCS
    _DLCS = dlcs


def db_dep():
    """Per-request DB session. Commits on success, rolls back on exception."""
    session = get_session()
    try:
        yield session
        session.commit()
    except Exception:
        session.rollback()
        raise
    finally:
        session.close()


def _err(status: int, code: str, message: str) -> HTTPException:
    return HTTPException(status_code=status, detail={"error_code": code, "message": message})


# ---------- DLC catalog ----------

@router.get("/dlcs", response_model=ListDLCsResponse, tags=["DLC"])
def list_dlcs() -> ListDLCsResponse:
    return ListDLCsResponse(
        dlcs=[
            DLCSummary(
                dlc_id=c.dlc_id,
                title=c.manifest["title"],
                description=c.manifest["description"],
                max_score=c.manifest["max_score"],
            )
            for c in _DLCS.values()
        ]
    )


# ---------- Playthrough lifecycle ----------

@router.post("/playthroughs", response_model=StartPlaythroughResponse, tags=["Playthrough"])
def start_playthrough(
    req: StartPlaythroughRequest, session: Session = Depends(db_dep)
) -> StartPlaythroughResponse:
    dlc = _DLCS.get(req.dlc_id)
    if dlc is None:
        raise _err(404, "DLC_NOT_FOUND", f"Unknown DLC {req.dlc_id!r}")
    p, scene = game_logic.start_or_resume(session, dlc, req.player_uuid)
    return StartPlaythroughResponse(playthrough=game_logic.to_schema(p), scene=scene)


@router.get(
    "/playthroughs/{playthrough_id}/scene",
    response_model=GetSceneResponse,
    tags=["Playthrough"],
)
def get_current_scene(
    playthrough_id: str = Path(...), session: Session = Depends(db_dep)
) -> GetSceneResponse:
    p = db_svc.get_playthrough(session, playthrough_id)
    if p is None:
        raise _err(404, "PLAYTHROUGH_NOT_FOUND", "Playthrough does not exist.")
    if p.status != "IN_PROGRESS":
        raise _err(409, "PLAYTHROUGH_COMPLETED",
                   "Playthrough has reached its ending. Call /ending instead.")
    dlc = _DLCS.get(p.dlc_id)
    if dlc is None or p.current_scene_id is None:
        raise _err(409, "INCONSISTENT_STATE", "Playthrough references unknown DLC or scene.")
    return GetSceneResponse(
        playthrough=game_logic.to_schema(p),
        scene=dlc.get_scene(p.current_scene_id),
    )


@router.post(
    "/playthroughs/{playthrough_id}/decisions",
    response_model=SubmitDecisionResponse,
    tags=["Playthrough"],
)
def submit_decision(
    body: SubmitDecisionRequest,
    playthrough_id: str = Path(...),
    session: Session = Depends(db_dep),
) -> SubmitDecisionResponse:
    p = db_svc.get_playthrough(session, playthrough_id)
    if p is None:
        raise _err(404, "PLAYTHROUGH_NOT_FOUND", "Playthrough does not exist.")
    dlc = _DLCS.get(p.dlc_id)
    if dlc is None:
        raise _err(409, "INCONSISTENT_STATE", "Playthrough references unknown DLC.")
    try:
        return game_logic.apply_decision(session, dlc, p, body)
    except ValueError as e:
        raise _err(409, "STATE_CONFLICT", str(e))


@router.get(
    "/playthroughs/{playthrough_id}/ending",
    response_model=GetEndingResponse,
    tags=["Playthrough"],
)
def get_ending(
    playthrough_id: str = Path(...), session: Session = Depends(db_dep)
) -> GetEndingResponse:
    p = db_svc.get_playthrough(session, playthrough_id)
    if p is None:
        raise _err(404, "PLAYTHROUGH_NOT_FOUND", "Playthrough does not exist.")
    dlc = _DLCS.get(p.dlc_id)
    if dlc is None:
        raise _err(409, "INCONSISTENT_STATE", "Playthrough references unknown DLC.")
    try:
        result = game_logic.resolve_ending(session, dlc, p)
    except ValueError as e:
        raise _err(409, "ENDING_NOT_REACHED", str(e))
    return GetEndingResponse(**result)


# ---------- Hermione's Notes ----------

@router.post("/hermione/query", response_model=HermioneQueryResponse, tags=["Hermione"])
def hermione_query(
    body: HermioneQueryRequest, session: Session = Depends(db_dep)
) -> HermioneQueryResponse:
    result = hermione.answer_query(
        session, body.player_uuid, body.query, body.playthrough_id
    )
    return HermioneQueryResponse(**result)
