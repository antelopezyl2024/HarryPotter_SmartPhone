"""Load DLC manifest, scenes, endings, and lore from filesystem.

Treats the JSON files under `data/dlcs/{dlc_id}/` as the authoritative source.
Validates each scene through the Pydantic Scene model on load — if any scene
file is malformed, startup fails loudly.
"""

from __future__ import annotations

import json
from pathlib import Path
from typing import Any

from app.api.schemas import Scene


class DLCContent:
    def __init__(
        self,
        manifest: dict[str, Any],
        scenes: dict[str, Scene],
        scene_definitions: dict[str, dict[str, Any]],
        endings: list[dict[str, Any]],
        lore: list[dict[str, Any]],
    ):
        self.manifest = manifest
        self.scenes = scenes
        self.scene_definitions = scene_definitions
        self.scene_order: list[str] = manifest["scene_order"]
        self.endings = endings
        self.lore = lore

    @property
    def dlc_id(self) -> str:
        return self.manifest["dlc_id"]

    def first_scene(self) -> Scene:
        return self.scenes[self.scene_order[0]]

    def get_scene(self, scene_id: str) -> Scene:
        return self.scenes[scene_id]

    def get_scene_definition(self, scene_id: str) -> dict[str, Any]:
        return self.scene_definitions[scene_id]

    def is_last_scene(self, scene_id: str) -> bool:
        return scene_id == self.scene_order[-1]

    def resolve_ending(self, total_score: int) -> dict[str, Any]:
        for e in self.endings:
            r = e["score_range"]
            if r["min"] <= total_score <= r["max"]:
                return e
        raise ValueError(f"No ending matches score {total_score} in DLC {self.dlc_id}")


def _validate_scene_scoring_fields(scene_data: dict[str, Any], path: Path) -> None:
    decision = scene_data.get("decision", {})
    mode = decision.get("mode")

    if mode in {"CHOICE", "EITHER"}:
        options = decision.get("choice", {}).get("options", [])
        for opt in options:
            if "is_correct" not in opt:
                raise ValueError(
                    f"{path}: choice option {opt.get('id')!r} is missing is_correct."
                )
            if not isinstance(opt["is_correct"], bool):
                raise ValueError(
                    f"{path}: choice option {opt.get('id')!r} has non-boolean "
                    "is_correct."
                )

    if mode in {"FREE_TEXT", "EITHER"}:
        free_text = decision.get("free_text", {})
        if "llm_classification_criteria" not in free_text:
            raise ValueError(f"{path}: free_text is missing llm_classification_criteria.")

        categories = free_text.get("categories")
        if not isinstance(categories, list) or not categories:
            raise ValueError(f"{path}: free_text.categories must be a non-empty list.")

        for cat in categories:
            if "is_correct" not in cat:
                raise ValueError(
                    f"{path}: free-text category {cat.get('id')!r} is missing "
                    "is_correct."
                )
            if not isinstance(cat["is_correct"], bool):
                raise ValueError(
                    f"{path}: free-text category {cat.get('id')!r} has non-boolean "
                    "is_correct."
                )


def load_dlc(dlc_dir: Path) -> DLCContent:
    manifest = json.loads((dlc_dir / "manifest.json").read_text(encoding="utf-8"))

    scenes: dict[str, Scene] = {}
    scene_definitions: dict[str, dict[str, Any]] = {}
    for scene_id in manifest["scene_order"]:
        path = dlc_dir / "scenes" / f"{scene_id}.json"
        scene_data = json.loads(path.read_text(encoding="utf-8"))
        _validate_scene_scoring_fields(scene_data, path)
        scene_definitions[scene_id] = scene_data
        scenes[scene_id] = Scene(**scene_data)

    endings = json.loads((dlc_dir / "endings.json").read_text(encoding="utf-8"))["endings"]
    lore = json.loads((dlc_dir / "lore.json").read_text(encoding="utf-8"))["entries"]

    return DLCContent(manifest, scenes, scene_definitions, endings, lore)


def load_all_dlcs(data_dir: Path) -> dict[str, DLCContent]:
    dlcs: dict[str, DLCContent] = {}
    if not data_dir.exists():
        return dlcs
    for sub in sorted(data_dir.iterdir()):
        if sub.is_dir() and (sub / "manifest.json").exists():
            content = load_dlc(sub)
            dlcs[content.dlc_id] = content
    return dlcs
