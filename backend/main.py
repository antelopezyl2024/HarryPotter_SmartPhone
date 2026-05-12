"""
Harry Potter game backend — in-memory FastAPI server for demo/testing.
Serves multiple DLCs with deterministic scoring (no LLM required).
"""
from __future__ import annotations

import json
import uuid
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Dict, List, Optional

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel

app = FastAPI(title="HarryPotter Game API")
app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_methods=["*"], allow_headers=["*"])

# ---------------------------------------------------------------------------
# Load all DLC data at startup
# ---------------------------------------------------------------------------

DLCS_ROOT = Path(__file__).parent.parent / "data" / "dlcs"

def _load_json(path: Path) -> Any:
    return json.loads(path.read_text())

# dlc_id -> { "manifest": ..., "endings": ..., "scenes": {scene_id: raw}, "scene_order": [...] }
DLC_REGISTRY: Dict[str, Dict] = {}

for dlc_dir in sorted(DLCS_ROOT.iterdir()):
    if not dlc_dir.is_dir():
        continue
    manifest_path = dlc_dir / "manifest.json"
    endings_path = dlc_dir / "endings.json"
    scenes_dir = dlc_dir / "scenes"
    if not (manifest_path.exists() and endings_path.exists() and scenes_dir.exists()):
        continue
    m = _load_json(manifest_path)
    scenes: Dict[str, Any] = {}
    for sf in sorted(scenes_dir.glob("*.json")):
        s = _load_json(sf)
        scenes[s["scene_id"]] = s
    DLC_REGISTRY[m["dlc_id"]] = {
        "manifest": m,
        "endings": _load_json(endings_path),
        "scenes": scenes,
        "scene_order": m["scene_order"],
    }

# ---------------------------------------------------------------------------
# In-memory state
# ---------------------------------------------------------------------------

class PlaythroughState(BaseModel):
    playthrough_id: str
    player_uuid: str
    dlc_id: str
    current_scene_index: int = 0
    total_score: int = 0
    status: str = "IN_PROGRESS"
    started_at: str = ""
    ended_at: Optional[str] = None
    ending_id: Optional[str] = None
    decisions: List[Dict] = []

DB: Dict[str, PlaythroughState] = {}

# ---------------------------------------------------------------------------
# Helper: convert raw scene JSON → API Scene dict
# ---------------------------------------------------------------------------

def _build_scene(raw: Dict) -> Dict:
    decision = dict(raw["decision"])
    # Strip internal fields not exposed to client
    if "choice" in decision:
        decision["choice"] = {
            "options": [
                {"id": o["id"], "text": o["text"]}
                for o in decision["choice"]["options"]
            ]
        }
    if "free_text" in decision:
        ft = decision["free_text"]
        decision["free_text"] = {"prompt": ft["prompt"], "max_chars": ft["max_chars"]}
    return {
        "scene_id": raw["scene_id"],
        "order": raw["order"],
        "title": raw["title"],
        "narrative": raw["narrative"],
        "decision": decision,
        "next_scene_id": raw.get("next_scene_id"),
    }


def _playthrough_dict(pt: PlaythroughState) -> Dict:
    scene_order = DLC_REGISTRY[pt.dlc_id]["scene_order"]
    return {
        "playthrough_id": pt.playthrough_id,
        "player_uuid": pt.player_uuid,
        "dlc_id": pt.dlc_id,
        "current_scene_id": scene_order[pt.current_scene_index] if pt.status == "IN_PROGRESS" else None,
        "total_score": pt.total_score,
        "status": pt.status,
        "started_at": pt.started_at,
        "ended_at": pt.ended_at,
        "ending_id": pt.ending_id,
    }


def _is_choice_correct(scene_raw: Dict, choice_id: str) -> bool:
    for opt in scene_raw["decision"]["choice"]["options"]:
        if opt["id"] == choice_id:
            return opt.get("is_correct", False)
    return False


def _classify_free_text(scene_raw: Dict, text: str) -> tuple[str, bool]:
    """Keyword-based classifier (no LLM). Returns (category_id, is_correct)."""
    text_lower = text.lower()
    sid = scene_raw["scene_id"]

    # --- Sirius Must Live ---
    if sid == "S1_THE_VISION":
        if any(w in text_lower for w in ["mirror", "snape", "dumbledore", "order", "mcgonagall", "verify", "check"]):
            return "SAFE_VERIFICATION", True
        if any(w in text_lower for w in ["wait", "careful", "gather", "think", "inform"]):
            return "CAUTIOUS_INVESTIGATE", True
        if any(w in text_lower for w in ["fly", "rush", "go", "leave", "ministry", "thestr"]):
            return "IMPULSIVE_RUSH", False

    elif sid == "S2_THE_REINFORCEMENT":
        if any(w in text_lower for w in ["snape", "patronus", "order", "coin", "signal", "warn", "note", "message", "mcgonagall"]):
            return "ORDER_SIGNAL", True
        if any(w in text_lower for w in ["only", "ourselves", "just us", "da only"]):
            return "DA_ONLY", False

    elif sid == "S3_THE_PROPHECY":
        if any(w in text_lower for w in ["leave", "flee", "run", "exit", "out", "escape", "ignore", "don't take", "do not take"]):
            return "DISSUADE_AND_FLEE", True
        if any(w in text_lower for w in ["take", "grab", "pick up", "lift", "carry"]):
            return "TAKE_AND_FLEE", False
        if any(w in text_lower for w in ["read", "study", "look", "listen", "examine"]):
            return "DELAY_AND_INSPECT", False

    elif sid == "S4_THE_BRINK_OF_DEATH":
        if any(w in text_lower for w in ["pull", "tackle", "grab", "accio", "levit", "summon", "drag"]):
            return "PROTECT_PULL", True
        if any(w in text_lower for w in ["protego", "shield", "impedimenta", "stupefy bellatrix", "expelliarmus"]):
            return "PROTECT_SPELL", True
        if any(w in text_lower for w in ["stupefy sirius", "attack", "avada"]):
            return "OFFENSIVE_ONLY", False

    elif sid == "S5_THE_TRIAL_OF_FATE":
        if any(w in text_lower for w in ["confess", "evidence", "truth", "inquiry", "public", "testify", "prove", "reveal", "wake", "revive"]):
            return "PUBLIC_PROOF", True
        if any(w in text_lower for w in ["flee", "escape", "vanish", "run", "hide"]):
            return "QUICK_ESCAPE", False
        if any(w in text_lower for w in ["wait", "stall", "think", "maybe"]):
            return "HESITATE", False

    # --- Remus Must Survive ---
    elif sid == "R1_THE_ARRIVAL":
        if any(w in text_lower for w in ["tonks", "astronomy", "tower", "together", "join", "help her", "go to her"]):
            return "GO_TO_TONKS", True
        if any(w in text_lower for w in ["great hall", "entrance", "front", "main", "alone", "without tonks"]):
            return "FRONT_LINE", False
        if any(w in text_lower for w in ["wait", "mcgonagall", "order", "assignment", "orders"]):
            return "WAIT_ORDERS", False

    elif sid == "R2_THE_CALLING":
        if any(w in text_lower for w in ["together", "both", "pair", "side by side", "with tonks", "tonks with", "join"]):
            return "TOGETHER", True
        if any(w in text_lower for w in ["split", "alone", "send tonks", "tonks to", "by myself", "separately", "flank"]):
            return "SPLIT", False
        if any(w in text_lower for w in ["wait", "backup", "reinforce", "more", "request"]):
            return "WAIT_BACKUP", False

    elif sid == "R3_THE_CORRIDOR":
        if any(w in text_lower for w in ["protego", "shield", "expelliarmus", "defend", "flank", "distance", "block"]):
            return "DEFENSIVE_FLANK", True
        if any(w in text_lower for w in ["both", "simultaneously", "together", "at once", "double", "two wands"]):
            return "SIMULTANEOUS", True
        if any(w in text_lower for w in ["charge", "rush", "close", "tackle", "grab", "run at"]):
            return "CHARGE", False
        if any(w in text_lower for w in ["stupefy", "attack", "curse", "hex", "blast"]):
            return "SINGLE_ATTACK", False

    elif sid == "R4_THE_BREACH":
        if any(w in text_lower for w in ["seal", "protego maxima", "barrier", "fall back", "retreat", "regroup", "close"]):
            return "SEAL_AND_FALL_BACK", True
        if any(w in text_lower for w in ["hold", "fight", "stay", "stand", "duel", "wave"]):
            return "HOLD_BY_FORCE", False
        if any(w in text_lower for w in ["split", "separate", "tonks goes", "send tonks", "divide"]):
            return "SPLIT_UP", False

    elif sid == "R5_THE_DAWN":
        if any(w in text_lower for w in ["fall back", "retreat", "withdraw", "rest", "survive", "teddy", "live", "regroup", "safe"]):
            return "FALL_BACK_TOGETHER", True
        if any(w in text_lower for w in ["stay", "hold", "fight", "front", "remain", "keep"]):
            return "HOLD_FRONT", False
        if any(w in text_lower for w in ["send tonks", "split", "separate", "alone", "tonks leaves", "remus stays"]):
            return "SEPARATE", False

    return "INVALID", False


def _feedback_for(scene_raw: Dict, choice_id: Optional[str], category_id: Optional[str], is_correct: bool) -> str:
    if choice_id:
        for opt in scene_raw["decision"]["choice"]["options"]:
            if opt["id"] == choice_id:
                return opt.get("feedback_hint", "Your choice has been recorded.")
    if is_correct:
        return "A wise decision. The threads of fate shift in your favour."
    dlc_hints = {
        "remus_must_survive": "That path leads to darkness. The Lupins' fate hangs by a thread.",
    }
    return "That path leads to shadows. Sirius's fate hangs by a thread."


def _compute_ending(dlc_id: str, total_score: int) -> Dict:
    endings = DLC_REGISTRY[dlc_id]["endings"]["endings"]
    for e in endings:
        r = e["score_range"]
        if r["min"] <= total_score <= r["max"]:
            return e
    return endings[-1]


def _now() -> str:
    return datetime.now(timezone.utc).isoformat()

# ---------------------------------------------------------------------------
# Routes
# ---------------------------------------------------------------------------

@app.get("/api/v1/dlcs")
def list_dlcs():
    return {
        "dlcs": [
            {
                "dlc_id": dlc["manifest"]["dlc_id"],
                "title": dlc["manifest"]["title"],
                "description": dlc["manifest"]["description"],
                "max_score": dlc["manifest"]["max_score"],
            }
            for dlc in DLC_REGISTRY.values()
        ]
    }


@app.post("/api/v1/playthroughs")
def start_playthrough(body: Dict):
    player_uuid = body.get("player_uuid", str(uuid.uuid4()))
    dlc_id = body.get("dlc_id", "sirius_must_live")
    if dlc_id not in DLC_REGISTRY:
        raise HTTPException(404, f"DLC '{dlc_id}' not found")
    pt = PlaythroughState(
        playthrough_id=str(uuid.uuid4()),
        player_uuid=player_uuid,
        dlc_id=dlc_id,
        started_at=_now(),
    )
    DB[pt.playthrough_id] = pt
    first_scene_id = DLC_REGISTRY[dlc_id]["scene_order"][0]
    return {
        "playthrough": _playthrough_dict(pt),
        "scene": _build_scene(DLC_REGISTRY[dlc_id]["scenes"][first_scene_id]),
    }


@app.get("/api/v1/playthroughs/{pt_id}/scene")
def get_scene(pt_id: str):
    pt = DB.get(pt_id)
    if not pt:
        raise HTTPException(404, "Playthrough not found")
    dlc = DLC_REGISTRY[pt.dlc_id]
    scene_id = dlc["scene_order"][pt.current_scene_index]
    return {
        "playthrough": _playthrough_dict(pt),
        "scene": _build_scene(dlc["scenes"][scene_id]),
    }


@app.post("/api/v1/playthroughs/{pt_id}/decisions")
def submit_decision(pt_id: str, body: Dict):
    pt = DB.get(pt_id)
    if not pt:
        raise HTTPException(404, "Playthrough not found")
    if pt.status != "IN_PROGRESS":
        raise HTTPException(400, "Playthrough already completed")

    dlc = DLC_REGISTRY[pt.dlc_id]
    scene_id = dlc["scene_order"][pt.current_scene_index]
    scene_raw = dlc["scenes"][scene_id]
    mode = body.get("mode", "CHOICE")

    is_correct = False
    choice_id = None
    category_id = None
    ai_classification = None

    if mode == "CHOICE":
        choice_id = body.get("choice_id", "")
        is_correct = _is_choice_correct(scene_raw, choice_id)
        feedback = _feedback_for(scene_raw, choice_id, None, is_correct)
    else:
        text = body.get("text", "")
        category_id, is_correct = _classify_free_text(scene_raw, text)
        feedback = _feedback_for(scene_raw, None, category_id, is_correct)
        ai_classification = {"category_id": category_id, "confidence": 0.92 if is_correct else 0.85}

    delta = 1 if is_correct else 0
    pt.total_score += delta
    pt.decisions.append({
        "scene_id": scene_id,
        "mode_used": mode,
        "is_correct": is_correct,
        "summary": scene_raw["title"] + (" — correct" if is_correct else " — incorrect"),
    })

    # Advance scene
    pt.current_scene_index += 1
    scene_order = dlc["scene_order"]
    is_last = pt.current_scene_index >= len(scene_order)
    next_scene = None

    if is_last:
        pt.status = "COMPLETED"
        pt.ended_at = _now()
        ending = _compute_ending(pt.dlc_id, pt.total_score)
        pt.ending_id = ending["id"]
        playthrough_status = "COMPLETED"
    else:
        next_scene_id = scene_order[pt.current_scene_index]
        next_scene = _build_scene(dlc["scenes"][next_scene_id])
        playthrough_status = "IN_PROGRESS"

    return {
        "is_correct": is_correct,
        "delta_score": delta,
        "total_score": pt.total_score,
        "narrative_feedback": feedback,
        "ai_classification": ai_classification,
        "next_scene": next_scene,
        "playthrough_status": playthrough_status,
    }


@app.get("/api/v1/playthroughs/{pt_id}/ending")
def get_ending(pt_id: str):
    pt = DB.get(pt_id)
    if not pt:
        raise HTTPException(404, "Playthrough not found")
    if pt.status != "COMPLETED":
        raise HTTPException(400, "Playthrough not yet complete")

    dlc = DLC_REGISTRY[pt.dlc_id]
    ending = _compute_ending(pt.dlc_id, pt.total_score)
    total_scenes = len(dlc["scene_order"])
    return {
        "ending_id": pt.ending_id,
        "title": ending["title"],
        "narrative": ending["narrative"],
        "summary": f"You scored {pt.total_score} out of {total_scenes} — {'the light holds' if pt.ending_id == 'SUCCESS' else 'the darkness wins'}.",
        "total_score": pt.total_score,
        "decisions_overview": pt.decisions,
    }


@app.post("/api/v1/hermione/query")
def hermione_query(body: Dict):
    query = body.get("query", "").lower()

    if "sirius" in query:
        answer = "Sirius is at Grimmauld Place. The vision Harry saw may not be what it seems — Voldemort has used Harry's mind connection before to plant false images."
    elif "mirror" in query or "two-way" in query:
        answer = "The two-way mirror Sirius gave Harry at Christmas can reach him directly without going anywhere. It hasn't been used yet — that's significant."
    elif "prophecy" in query or "orb" in query or "glass ball" in query:
        answer = "Only the subject of a prophecy can retrieve it from the shelf without harm. But remember — Voldemort wants Harry to go there. The orb is bait, not a prize."
    elif "veil" in query or "arch" in query or "death chamber" in query:
        answer = "The Veil in the Death Chamber is irreversible. No spell, no magic known to wizardkind can retrieve someone who passes through it. Keeping everyone away from it is the priority."
    elif "bellatrix" in query or "lestrange" in query:
        answer = "Bellatrix Lestrange is Voldemort's most devoted and unpredictable servant. She duels to kill. Direct confrontation without backup is extremely dangerous."
    elif "fudge" in query or "minister" in query or "cornelius" in query:
        answer = "Cornelius Fudge is stubborn but not cruel. He has denied Voldemort's return all year. Public evidence — witnesses, a confession, something undeniable — is the only thing that moves him."
    elif "harry" in query:
        answer = "Harry has a mental connection to Voldemort through his scar. Voldemort has exploited this before. Any vision Harry sees involving Sirius should be verified before acting on it."
    elif "voldemort" in query or "dark lord" in query or "you-know-who" in query or "he-who" in query:
        answer = "Voldemort has returned, though the Ministry refuses to accept it. He wants the prophecy from the Department of Mysteries and will use any means — including Harry's mind — to get someone to retrieve it."
    elif "dumbledore" in query or "albus" in query:
        answer = "Dumbledore leads the Order of the Phoenix and knows far more than he reveals. He placed protective enchantments at Grimmauld Place. If Harry can reach him, many dangers can be avoided entirely."
    elif "snape" in query or "severus" in query or "occlumency" in query:
        answer = "Snape is teaching Harry Occlumency to block Voldemort from his mind. He is also a member of the Order of the Phoenix. In a crisis, a Patronus message can reach him quickly."
    elif "order" in query or "phoenix" in query or "grimmauld" in query:
        answer = "The Order of the Phoenix operates from 12 Grimmauld Place. Members include Lupin, Moody, Tonks, and others. They can mobilise quickly if warned — a Patronus message is the fastest way to reach them."
    elif "lupin" in query or "remus" in query or "moody" in query or "tonks" in query:
        answer = "Lupin, Moody, and Tonks are all Order members. Trained, experienced, and far better equipped for the Department of Mysteries than a group of students. Getting word to them could change everything."
    elif "neville" in query or "luna" in query or "ginny" in query or "ron" in query:
        answer = "Your friends are brave, but the Department of Mysteries is no place for students alone. Every extra person who comes increases the risk. Think carefully about who truly needs to be there."
    elif "umbridge" in query or "dolores" in query:
        answer = "Dolores Umbridge holds power at Hogwarts under Ministry authority. She is dangerous and not to be trusted. Avoid revealing your plans to anyone who might report back to her."
    elif "dementor" in query or "patronus" in query:
        answer = "A Patronus can serve as a messenger within the Order — each member's Patronus is unique and cannot be faked. It's also the only known defence against Dementors, which the Ministry has deployed around Hogwarts."
    elif "department" in query or "mysteries" in query or "ministry" in query:
        answer = "The Department of Mysteries is deep within the Ministry of Magic. It contains rooms dedicated to Time, Space, Thought, Death, and Prophecy. Few who enter unsupervised leave without consequences."
    elif "death eater" in query or "death eaters" in query or "malfoy" in query or "lucius" in query:
        answer = "Death Eaters are Voldemort's inner circle, skilled and ruthless. Lucius Malfoy in particular has access to the Ministry. If you encounter them, outnumbering them or outrunning them is safer than outfighting them."
    elif "occlumency" in query or "legilimency" in query or "mind" in query or "vision" in query:
        answer = "Voldemort can send false visions through Harry's mind connection. Occlumency could have blocked this. Any vision that conveniently tells Harry exactly where to go should be treated with deep suspicion."
    elif "wand" in query or "spell" in query or "charm" in query or "curse" in query:
        answer = "In the Department of Mysteries, conventional duelling is chaotic. Shields, disarming, and escape charms are more reliable than offensive curses in enclosed, unfamiliar spaces."
    elif "time turner" in query or "time" in query:
        answer = "Time Turners are kept in the Department of Mysteries — but using one without authorisation has unpredictable consequences. The Ministry tracks their use closely."
    elif "grimmauld" in query or "headquarters" in query or "safe house" in query:
        answer = "12 Grimmauld Place is the Order's headquarters, hidden under a Fidelius Charm. Sirius lives there but has been confined — which is why verifying his safety without going there matters."
    elif "hogwarts" in query or "school" in query or "castle" in query:
        answer = "Hogwarts is currently under Umbridge's supervision. Dumbledore is absent. Most of the teachers sympathise with Harry, but their hands are tied under Ministry oversight."
    else:
        answer = "I'm not certain about that specifically, but here's what I know: in the Order of the Phoenix, haste is Voldemort's greatest weapon against Harry. Verify first, act second — there is almost always another way."

    return {
        "answer": answer,
        "sources": [{"lore_id": "lore_001", "title": "Order of the Phoenix — Department of Mysteries"}],
    }


@app.get("/health")
def health():
    return {"status": "ok"}
