"""
Pydantic schemas for the HP Project backend REST API.

This module is the single source of truth for the request/response shapes
exchanged between the Android client and the backend. The OpenAPI document
shipped to the client team (`backend/openapi.yaml`) mirrors these models.

Conventions:
- All player-facing string fields are English.
- IDs are strings; UUIDs are formatted as RFC 4122.
- Datetimes are ISO 8601 (UTC).
"""

from __future__ import annotations

from datetime import datetime
from enum import Enum
from typing import Annotated, List, Literal, Optional, Union

from pydantic import BaseModel, Field


# ---------------------------------------------------------------------------
# Enums
# ---------------------------------------------------------------------------

class DecisionMode(str, Enum):
    """Which mode the player actually used to answer."""
    CHOICE = "CHOICE"
    FREE_TEXT = "FREE_TEXT"


class PlaythroughStatus(str, Enum):
    IN_PROGRESS = "IN_PROGRESS"
    COMPLETED = "COMPLETED"


class EndingId(str, Enum):
    SUCCESS = "SUCCESS"
    FAIL = "FAIL"


# ---------------------------------------------------------------------------
# Scene definition
# ---------------------------------------------------------------------------

class ChoiceOption(BaseModel):
    id: str = Field(..., description="Option identifier, e.g. 'A', 'B'.")
    text: str = Field(..., description="Player-facing English option text.")


class ChoiceConfig(BaseModel):
    options: List[ChoiceOption]


class FreeTextConfig(BaseModel):
    prompt: str = Field(..., description="Player-facing English prompt.")
    max_chars: int = Field(..., ge=1, description="Hard limit on input length.")


class ChoiceOnlyDecision(BaseModel):
    """Scene that only exposes A/B/C buttons. No free-text input."""
    mode: Literal["CHOICE"] = "CHOICE"
    choice: ChoiceConfig


class FreeTextOnlyDecision(BaseModel):
    """Scene that only accepts free-text input. No buttons."""
    mode: Literal["FREE_TEXT"] = "FREE_TEXT"
    free_text: FreeTextConfig


class EitherDecision(BaseModel):
    """Scene that exposes both buttons and free-text input. The player picks one."""
    mode: Literal["EITHER"] = "EITHER"
    choice: ChoiceConfig
    free_text: FreeTextConfig


SceneDecision = Annotated[
    Union[ChoiceOnlyDecision, FreeTextOnlyDecision, EitherDecision],
    Field(discriminator="mode"),
]


class Scene(BaseModel):
    scene_id: str
    order: int = Field(..., ge=1)
    title: str
    narrative: str
    decision: SceneDecision
    next_scene_id: Optional[str] = Field(
        None, description="Null on the last scene of the DLC."
    )


# ---------------------------------------------------------------------------
# DLC catalog
# ---------------------------------------------------------------------------

class DLCSummary(BaseModel):
    dlc_id: str
    title: str
    description: str
    max_score: int


class ListDLCsResponse(BaseModel):
    dlcs: List[DLCSummary]


# ---------------------------------------------------------------------------
# Playthrough lifecycle
# ---------------------------------------------------------------------------

class Playthrough(BaseModel):
    playthrough_id: str
    player_uuid: str
    dlc_id: str
    current_scene_id: Optional[str]
    total_score: int = Field(..., ge=0)
    status: PlaythroughStatus
    started_at: datetime
    ended_at: Optional[datetime] = None
    ending_id: Optional[EndingId] = None


class StartPlaythroughRequest(BaseModel):
    player_uuid: str = Field(..., description="Client-generated UUID, RFC 4122.")
    dlc_id: str


class StartPlaythroughResponse(BaseModel):
    playthrough: Playthrough
    scene: Scene = Field(..., description="The first (or resumed) scene.")


class GetSceneResponse(BaseModel):
    playthrough: Playthrough
    scene: Scene


# ---------------------------------------------------------------------------
# Decision submission (discriminated union on `mode`)
# ---------------------------------------------------------------------------

class ChoiceDecisionRequest(BaseModel):
    scene_id: str
    mode: Literal["CHOICE"] = "CHOICE"
    choice_id: str = Field(..., description="One of the option IDs from the scene.")


class FreeTextDecisionRequest(BaseModel):
    scene_id: str
    mode: Literal["FREE_TEXT"] = "FREE_TEXT"
    text: str = Field(..., min_length=1)


SubmitDecisionRequest = Annotated[
    Union[ChoiceDecisionRequest, FreeTextDecisionRequest],
    Field(discriminator="mode"),
]


class AIClassification(BaseModel):
    """Returned only when the player used FREE_TEXT mode."""
    category_id: str
    confidence: float = Field(..., ge=0.0, le=1.0)


class SubmitDecisionResponse(BaseModel):
    is_correct: bool
    delta_score: int = Field(..., ge=0, le=1)
    total_score: int
    narrative_feedback: str = Field(..., description="LLM-generated, English.")
    ai_classification: Optional[AIClassification] = None
    next_scene: Optional[Scene] = Field(
        None, description="Null when the playthrough has reached its ending."
    )
    playthrough_status: PlaythroughStatus


# ---------------------------------------------------------------------------
# Ending
# ---------------------------------------------------------------------------

class DecisionLogEntry(BaseModel):
    scene_id: str
    mode_used: DecisionMode
    is_correct: bool
    summary: str = Field(..., description="Short English summary of the player's action.")


class GetEndingResponse(BaseModel):
    ending_id: EndingId
    title: str
    narrative: str
    summary: str = Field(..., description="LLM-generated personalized summary, English.")
    total_score: int
    decisions_overview: List[DecisionLogEntry]


# ---------------------------------------------------------------------------
# Hermione's Notes (separate RAG module)
# ---------------------------------------------------------------------------

class HermioneQueryRequest(BaseModel):
    player_uuid: str
    query: str = Field(..., min_length=1, max_length=300)
    playthrough_id: Optional[str] = Field(
        None,
        description="Optional. Lets the backend log the query against a session.",
    )


class HermioneSource(BaseModel):
    lore_id: str
    title: str


class HermioneQueryResponse(BaseModel):
    answer: str = Field(
        ...,
        description="English hint. Does not directly reveal correct decisions.",
    )
    sources: List[HermioneSource]


# ---------------------------------------------------------------------------
# Errors
# ---------------------------------------------------------------------------

class ErrorResponse(BaseModel):
    error_code: str
    message: str
    details: Optional[dict] = None
