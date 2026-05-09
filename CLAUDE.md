# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository state

The artifacts in this repo are:
- `Project Requirements.pdf` — functional + non-functional requirements (read this first when implementing a feature).
- `Project_ High Level Architecture Design ...pdf` — architecture narrative + Scene 1/2/3 data-flow diagrams + sequence diagram. **Treat this PDF as the authoritative source for architecture and DFDs** — the diagrams are embedded inside it; there is no separate `diagrams/` folder.
- `data/dlcs/sirius_must_live/` — content data for the demo DLC (Yuling-authored): `manifest.json`, five scene JSONs under `scenes/`, `endings.json`, `lore.json` (RAG corpus), and `scenes_design.md` (human-readable design doc).
- `CLAUDE.md` — this file.

There is no backend source code or Android scaffolding yet. When asked to "implement" or "scaffold" anything, surface that fact rather than assuming a directory layout.

## Product, in one paragraph

**Trapped in Harry Potter: Rewrite the Fate** — an Android narrative-adventure game (team Nimbus). Players move through scripted scenes and try to alter canonical deaths. Core gameplay is **deterministic and rule-based** (scene progression, branching, scoring, endings); LLM + RAG layers sit on top for narrative feedback, free-text intent classification, and an in-game knowledge assistant ("Hermione's Notes"). Scope is a prototype/demo; auth/user management is explicitly out of scope.

**Demo scope (this submission):** one DLC, *Sirius Black Must Live*, with five fixed-order scenes and two endings (`SUCCESS` ≥ 4, `FAIL` ≤ 3) under a binary +1/0 per-decision scoring model. Auth, multi-DLC content, and cloud deployment are out of scope.

## Architecture

Hybrid client/cloud system. Android client ⇄ REST ⇄ backend. The backend is split into independent services so that **rule-based logic and AI logic stay separated** — this separation is a hard requirement (NFR 2.6), not a stylistic preference. Do not let AI output drive scoring, branching, or end-state determination.

Backend services (from the High Level Architecture Design):
- **API Gateway** — single entry point; routes client requests.
- **Game Logic Service** — deterministic core: scene control, branching, score calculation, ending evaluation, validation of AI results before they affect state.
- **AI Service** — prompt construction, narrative feedback generation, free-text input classification. Internally has Retriever Interface, Context Builder, Prompt Builder, Generator (see Scene 3 DFD).
- **RAG Knowledge Service** — retrieval + knowledge augmentation against the Knowledge Base.
- **DB Service** — structured game data (player progress, scene state, records).
- **Knowledge Base (DB/S3)** — lore documents and retrieval resources for RAG.
- **External LLM API** — OpenAI per the architecture doc.

**Cloud platform.** The architecture PDF specifies **AWS** (S3 for the Knowledge Base, AWS Cognito reserved for future auth). The "e.g., GCP" in the requirements doc is illustrative only, not the chosen platform. The current submission, however, runs **locally + ngrok** for the demo — no cloud deployment is in scope. AWS is the production target if the project ever leaves prototype.

## Two player-facing surfaces

The Android app has two distinct interaction entry points. Don't conflate them.

### 1. Storyline (the DLC scene loop)

Each scene is a single decision point. The decision can accept input in two modes — `CHOICE` (button options) and `FREE_TEXT` (LLM-classified input) — controlled per-scene by `decision.mode`:
- `CHOICE` — buttons only, no AI in the scoring path.
- `FREE_TEXT` — text input only; the AI Service classifies the input into one of the scene's predefined categories.
- `EITHER` — both UI paths exposed; the player picks one mode per scene. **All five scenes in the current demo use `EITHER`.**

Scoring is **binary per scene** (+1 for a correct answer, +0 otherwise). After all five scenes, the playthrough resolves to one of two endings keyed by total score (see `data/dlcs/sirius_must_live/endings.json`).

The DFD numbering in the architecture PDF (Scene 1 = 7 arrows, Scene 2 = 11 arrows) maps to **the two input modes within this surface**, not to two separate scene types.

### 2. Hermione's Notes (separate RAG module)

A standalone in-app feature with its own UI entry. The player can open it at any time, ask a question about the world, and receive an LLM-generated hint backed by retrieval from the Knowledge Base. **It does not advance the scene state, mutate the score, or branch the storyline.** Hints must not directly reveal correct decisions (FR 1.6).

This corresponds to the Scene 3 DFD (16 arrows) in the architecture PDF — the validation gate at step (11) (`validate intent & decide outcome`) is preserved as a function call before any response leaves the backend, ensuring the LLM cannot return spoilery content.

### What stays the same in both surfaces

**Game Logic owns all state writes.** AI Service and RAG Service never call the DB directly; they return structured results to Game Logic, which validates and persists. This is a hard requirement (NFR 2.6) and is what keeps LLM non-determinism from leaking into scores or branches.

## Implementation notes for the demo

- **Stack**: Python 3.11 + FastAPI monolith, SQLite for game state, embedded ChromaDB for RAG, OpenAI (`gpt-4o-mini` for chat/classification, `text-embedding-3-small` for embeddings). Local execution + ngrok for the demo.
- **Logical separation, physical monolith**: the architecture PDF's five backend services live as Python packages within one FastAPI app — not as separate processes. Module boundaries enforce NFR 2.6's rule/AI separation.
- **Language**: All player-facing text and LLM output is in **English only** (scene narratives, choice text, AI feedback, ending narratives, Hermione's Notes answers). LLM prompts must include explicit "respond in English" instructions. Working communication, code comments, and READMEs are in Chinese where natural to the team.
- **Player identity**: client-generated UUID stored in Android `SharedPreferences`, sent on every request as `player_uuid`. No registration, no auth.
- **Roles**: Yuling owns Backend + AI + RAG + Data Schema. The teammate (Shilpa) owns the Android client (UI, animation, Retrofit integration). The REST contract is the boundary.

## Design constraints worth keeping in mind

- Game content (scenes, choices, lore) lives in structured editable JSON under `data/dlcs/{dlc_id}/` (NFR 2.6). Treat scene definitions as data, not code.
- Scoring in the current DLC is **binary per scene** (+1 / 0). Total score determines the ending. Don't add weighted or partial scoring without checking with the team.
- AI response budget is 2–5s end-to-end (NFR 2.1). Cache or precompute where possible.
- LLM output is non-deterministic — control via prompt design *and* rule-based validation in Game Logic (Constraints §4). Always parse/validate AI output before acting on it.
- API keys must not ship in the Android client (NFR 2.5). All LLM calls go through the backend.
- Narrative is modular for DLC expansion (NFR 2.4) — keep scene graphs additive; new DLCs go under `data/dlcs/{new_dlc_id}/`.
