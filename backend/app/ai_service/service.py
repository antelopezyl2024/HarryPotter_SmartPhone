"""AI Service: classification, narrative feedback, Hermione's hint, ending summary.

All functions are sync. Call sites are FastAPI handlers running in a worker
thread; OpenAI's HTTP client handles its own concurrency.
"""

from __future__ import annotations

import json
import logging
from typing import Any

from app.ai_service.openai_client import get_client
from app.ai_service.prompts import (
    CLASSIFY_FREE_TEXT_SYSTEM,
    ENDING_SUMMARY_SYSTEM,
    HERMIONE_NOTES_SYSTEM,
    NARRATIVE_FEEDBACK_SYSTEM,
)
from app.config import settings

log = logging.getLogger(__name__)


def classify_free_text(
    *,
    scene_title: str,
    scene_narrative: str,
    prompt: str,
    criteria: str,
    categories: list[dict[str, Any]],
    player_text: str,
) -> dict[str, Any]:
    """Classify the player's free-text input into one of the scene's categories.

    Returns: {"category_id": str, "confidence": float}. Falls back to INVALID
    if the LLM returns an out-of-vocabulary id.
    """
    cat_block = "\n".join(f"- {c['id']}: {c['label']}" for c in categories)
    user_prompt = (
        f"Scene: {scene_title}\n"
        f"Scene context: {scene_narrative}\n"
        f"Decision prompt: {prompt}\n"
        f"Classification criteria: {criteria}\n"
        f"Allowed categories:\n{cat_block}\n\n"
        f'Player input: """{player_text}"""\n\n'
        f"Classify the player input. Respond with JSON only."
    )

    resp = get_client().chat.completions.create(
        model=settings.openai_chat_model,
        messages=[
            {"role": "system", "content": CLASSIFY_FREE_TEXT_SYSTEM},
            {"role": "user", "content": user_prompt},
        ],
        response_format={"type": "json_object"},
        temperature=0.0,
    )
    raw = resp.choices[0].message.content or "{}"
    try:
        parsed = json.loads(raw)
    except json.JSONDecodeError:
        log.warning("classify_free_text: unparseable JSON: %r", raw)
        return {"category_id": "INVALID", "confidence": 0.0}

    cat_id = parsed.get("category_id")
    valid_ids = {c["id"] for c in categories}
    if cat_id not in valid_ids:
        log.warning("classify_free_text: out-of-vocab category %r", cat_id)
        return {"category_id": "INVALID", "confidence": 0.0}

    try:
        conf = float(parsed.get("confidence", 0.5))
    except (TypeError, ValueError):
        conf = 0.5
    conf = max(0.0, min(1.0, conf))

    return {"category_id": cat_id, "confidence": conf}


def generate_narrative_feedback(
    *,
    scene_title: str,
    scene_narrative: str,
    action_summary: str,
    is_correct: bool,
    hint: str,
) -> str:
    user_prompt = (
        f"Scene: {scene_title}\n"
        f"Scene context: {scene_narrative}\n"
        f"Player's action: {action_summary}\n"
        f'The action was {"correct" if is_correct else "a mistake"} for surviving the scene.\n'
        f"Hint about why (do not echo this verbatim, weave it into narration): {hint}\n\n"
        f"Write the narrative response."
    )
    resp = get_client().chat.completions.create(
        model=settings.openai_chat_model,
        messages=[
            {"role": "system", "content": NARRATIVE_FEEDBACK_SYSTEM},
            {"role": "user", "content": user_prompt},
        ],
        temperature=0.7,
    )
    return (resp.choices[0].message.content or "").strip()


def generate_hermione_answer(query: str, retrieved_lore: list[dict[str, Any]]) -> str:
    if not retrieved_lore:
        return (
            "I haven't read anything that touches on that yet — try a more specific question."
        )
    lore_block = "\n\n".join(
        f"[{l['title']}]\n{l['text']}" for l in retrieved_lore
    )
    user_prompt = (
        f"Player question: {query}\n\n"
        f"Retrieved lore passages:\n{lore_block}\n\n"
        f"Compose Hermione's answer."
    )
    resp = get_client().chat.completions.create(
        model=settings.openai_chat_model,
        messages=[
            {"role": "system", "content": HERMIONE_NOTES_SYSTEM},
            {"role": "user", "content": user_prompt},
        ],
        temperature=0.5,
    )
    return (resp.choices[0].message.content or "").strip()


def generate_ending_summary(
    decisions: list[dict[str, Any]], ending_id: str, total_score: int
) -> str:
    decisions_block = "\n".join(
        f"- {d['scene_id']} ({d['mode_used']}): {d['summary']} — "
        f"{'CORRECT' if d['is_correct'] else 'MISTAKE'}"
        for d in decisions
    )
    user_prompt = (
        f"Decisions made:\n{decisions_block}\n\n"
        f"Ending: {ending_id} (score {total_score} out of 5).\n\n"
        f"Write the personalized summary."
    )
    resp = get_client().chat.completions.create(
        model=settings.openai_chat_model,
        messages=[
            {"role": "system", "content": ENDING_SUMMARY_SYSTEM},
            {"role": "user", "content": user_prompt},
        ],
        temperature=0.7,
    )
    return (resp.choices[0].message.content or "").strip()
