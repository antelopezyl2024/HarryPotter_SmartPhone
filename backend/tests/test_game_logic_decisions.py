from __future__ import annotations

from pathlib import Path

from app.api.schemas import ChoiceDecisionRequest
from app.db_service.models import Playthrough as PlaythroughDB
from app.game_logic.content_loader import load_dlc
from app.game_logic import service as game_service
from app.game_logic.scoring import is_choice_correct


PROJECT_ROOT = Path(__file__).resolve().parents[2]
DLC_DIR = PROJECT_ROOT / "data" / "dlcs" / "sirius_must_live"


def test_public_scene_omits_scoring_fields_but_internal_definition_keeps_them():
    dlc = load_dlc(DLC_DIR)

    public_scene = dlc.get_scene("S1_THE_VISION").model_dump()
    public_option = public_scene["decision"]["choice"]["options"][0]
    assert "is_correct" not in public_option
    assert "feedback_hint" not in public_option

    internal_scene = dlc.get_scene_definition("S1_THE_VISION")
    internal_option = internal_scene["decision"]["choice"]["options"][0]
    assert "is_correct" in internal_option
    assert "feedback_hint" in internal_option
    is_correct, hint = is_choice_correct(internal_scene, "B")
    assert is_correct is True
    assert "Sirius answers" in hint


def test_apply_choice_decision_uses_internal_definition_for_scoring(monkeypatch):
    dlc = load_dlc(DLC_DIR)
    playthrough = PlaythroughDB(
        playthrough_id="pt-1",
        player_uuid="player-1",
        dlc_id=dlc.dlc_id,
        current_scene_id="S1_THE_VISION",
        total_score=0,
        status="IN_PROGRESS",
    )
    logs: list[dict] = []

    monkeypatch.setattr(
        game_service.ai_svc,
        "generate_narrative_feedback",
        lambda **kwargs: "The vision is checked before anyone runs into danger.",
    )
    monkeypatch.setattr(
        game_service.db_svc,
        "write_decision_log",
        lambda session, **kwargs: logs.append(kwargs),
    )

    response = game_service.apply_decision(
        session=None,
        dlc=dlc,
        p=playthrough,
        request=ChoiceDecisionRequest(
            scene_id="S1_THE_VISION",
            mode="CHOICE",
            choice_id="B",
        ),
    )

    assert response.is_correct is True
    assert response.delta_score == 1
    assert response.total_score == 1
    assert playthrough.current_scene_id == "S2_THE_REINFORCEMENT"
    assert logs[0]["is_correct"] is True
    assert logs[0]["payload"] == {"choice_id": "B"}
