# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository state

This repository currently contains **design artifacts only** — no source code, build tooling, or tests exist yet. There is no `package.json`, `build.gradle`, etc. Do not invent build/test commands.

The artifacts are:
- `Project Requirements.pdf` — functional + non-functional requirements (read this first when implementing a feature).
- `Project_ High Level Architecture Design ...pdf` / `.docx` — architecture narrative + Scene 1/2/3 data-flow diagrams + sequence diagram.
- `diagrams/` — source files for the diagrams above:
  - `component_diagram.pu`, `sequence_diagram.pu` — PlantUML.
  - `basic_selection.drawio`, `free_text_input.drawio`, `RAG.drawio` — drawio sources, each with a rendered `.png` next to it.
  - `data_flow_diagram.pu` is currently empty.

When asked to "implement" or "scaffold" anything, check whether scaffolding exists yet. If not, surface that fact rather than assuming a directory layout.

## Product, in one paragraph

**Trapped in Harry Potter: Rewrite the Fate** — an Android narrative-adventure game (team Nimbus). Players move through scripted scenes and try to alter canonical deaths. Core gameplay is **deterministic and rule-based** (scene progression, branching, scoring, endings); LLM + RAG layers sit on top for narrative feedback, free-text intent classification, and an in-game knowledge assistant ("Hermione's Notes"). Scope is a prototype/demo; auth/user management is explicitly out of scope.

## Architecture

Hybrid client/cloud system. Android client ⇄ REST ⇄ cloud backend. The backend is split into independent services so that **rule-based logic and AI logic stay separated** — this separation is a hard requirement (NFR 2.6), not a stylistic preference. Do not let AI output drive scoring, branching, or end-state determination.

Backend services (from the High Level Architecture Design):
- **API Gateway** — single entry point; routes client requests.
- **Game Logic Service** — deterministic core: scene control, branching, score calculation, ending evaluation, validation of AI results before they affect state.
- **AI Service** — prompt construction, narrative feedback generation, free-text input classification. Internally has Retriever Interface, Context Builder, Prompt Builder, Generator (see Scene 3 DFD).
- **RAG Knowledge Service** — retrieval + knowledge augmentation against the Knowledge Base.
- **DB Service** — structured game data (player progress, scene state, records).
- **Knowledge Base (DB/S3)** — lore documents and retrieval resources for RAG.
- **External LLM API** — OpenAI per the architecture doc.

**Cloud platform discrepancy to resolve before deploying:** the architecture doc says AWS (with S3, AWS Cognito mentioned for future auth); the requirements doc says GCP. Confirm with the team before picking managed services.

## The three gameplay flows (each is a separate DFD)

These are the canonical interaction patterns. New features generally extend one of them — don't invent a fourth flow without checking the design docs.

1. **Basic selection (A/B/C/D)** — `diagrams/basic_selection.drawio`. Client → API Gateway → Game Logic → Game DB (read state, write result). No AI involvement. Cheap, deterministic.
2. **Free-text input** — `diagrams/free_text_input.drawio`. Adds the AI Service: Game Logic forwards player input + context to AI, which calls the External LLM API to **classify** the input into predefined strategy categories (e.g., tactical / defensive / invalid). Game Logic then validates and applies the classification to state. The classification *influences* outcome; it does not replace the rule engine.
3. **Knowledge query (RAG, "Hermione's Notes")** — `diagrams/RAG.drawio` and `sequence_diagram.pu`. AI Service calls the RAG Retriever, which pulls from the Knowledge Base; the Context Builder assembles augmented context; the LLM generates the response; Game Logic validates intent and decides outcome before responding. Hints must not directly reveal correct decisions (FR 1.6).

In all three flows, **Game Logic owns state writes** — AI/RAG never write to the DB directly.

## Design constraints worth keeping in mind

- Game content (scenes, choices) is required to live in a structured editable format, JSON cited as the example (NFR 2.6). Treat scene/choice definitions as data, not code.
- AI response budget is 2–5s end-to-end (NFR 2.1). Cache or precompute where possible.
- LLM output is non-deterministic — control via prompt design *and* rule-based validation in Game Logic (Constraints §4). Always parse/validate AI output before acting on it.
- API keys must not ship in the Android client (NFR 2.5). All LLM calls go through the backend.
- Narrative is modular for DLC expansion (NFR 2.4) — keep scene graphs additive.

## Working with the diagrams

- `.pu` files are PlantUML — render with the `plantuml` CLI or any PlantUML renderer. Edit the `.pu` source, not the PNG.
- `.drawio` files open in [diagrams.net](https://app.diagrams.net) (or the VS Code drawio extension). Each has a sibling `.drawio.png` export; if you change the source, regenerate the PNG.
- The DFDs use a numbered-arrow convention (e.g. "(1) read current state", "(2) return state") — preserve that numbering when extending a diagram so the narrative in the design doc still matches.
