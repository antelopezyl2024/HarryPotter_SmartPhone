"""RAG retrieval. Thin wrapper around the ChromaDB collection."""

from __future__ import annotations

from typing import Any

from app.rag_service.knowledge_loader import get_collection


def retrieve(query: str, top_k: int = 4) -> list[dict[str, Any]]:
    """Return the top-k most relevant lore entries for `query`.

    Output: [{"id": str, "title": str, "text": str}, ...]
    Empty list if the collection has no entries that match.
    """
    coll = get_collection()
    res = coll.query(query_texts=[query], n_results=top_k)

    if not res["ids"] or not res["ids"][0]:
        return []

    out: list[dict[str, Any]] = []
    for i, doc_id in enumerate(res["ids"][0]):
        out.append(
            {
                "id": doc_id,
                "title": res["metadatas"][0][i].get("title", ""),
                "text": res["documents"][0][i],
            }
        )
    return out
