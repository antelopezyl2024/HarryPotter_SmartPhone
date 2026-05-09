"""Hermione's Notes — separate RAG-only Q&A surface.

Does not advance scene state, mutate score, or branch the storyline. Logs the
query and retrieved sources so that ending summaries (or future analytics) can
reference them.
"""

from __future__ import annotations

from typing import Any

from sqlalchemy.orm import Session

from app.ai_service import service as ai_svc
from app.db_service import service as db_svc
from app.rag_service import service as rag_svc


def answer_query(
    session: Session,
    player_uuid: str,
    query: str,
    playthrough_id: str | None,
) -> dict[str, Any]:
    retrieved = rag_svc.retrieve(query, top_k=4)
    answer = ai_svc.generate_hermione_answer(query, retrieved)

    db_svc.write_hermione_query(
        session,
        player_uuid=player_uuid,
        playthrough_id=playthrough_id,
        query=query,
        answer=answer,
        retrieved_lore_ids=[r["id"] for r in retrieved],
    )

    return {
        "answer": answer,
        "sources": [{"lore_id": r["id"], "title": r["title"]} for r in retrieved],
    }
