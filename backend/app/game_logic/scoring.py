"""Pure functions for evaluating a player decision against scene definitions.

These are deterministic and live entirely in the rule engine. AI output (free-
text classification) is fed in as a `category_id` after the AI Service has
already produced it; scoring just looks it up against the scene's category
table.
"""

from __future__ import annotations

from typing import Any


def is_choice_correct(scene_dict: dict[str, Any], choice_id: str) -> tuple[bool, str]:
    """Resolve a button choice. Returns (is_correct, feedback_hint)."""
    choice_cfg = scene_dict["decision"].get("choice")
    if not choice_cfg:
        raise ValueError("Scene has no CHOICE config; cannot accept a choice payload.")
    for opt in choice_cfg["options"]:
        if opt["id"] == choice_id:
            return bool(opt["is_correct"]), opt.get("feedback_hint", "")
    raise ValueError(f"Choice id {choice_id!r} not in scene options.")


def is_category_correct(
    scene_dict: dict[str, Any], category_id: str
) -> tuple[bool, str]:
    """Resolve a free-text category. Returns (is_correct, label)."""
    ft = scene_dict["decision"].get("free_text")
    if not ft:
        raise ValueError("Scene has no FREE_TEXT config; cannot accept text payload.")
    for cat in ft["categories"]:
        if cat["id"] == category_id:
            return bool(cat["is_correct"]), cat["label"]
    raise ValueError(f"Category id {category_id!r} not in scene categories.")
