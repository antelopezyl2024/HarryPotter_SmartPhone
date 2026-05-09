"""Lazy-initialized OpenAI client.

The client is built on first use rather than at import time, so the FastAPI
app can boot even when OPENAI_API_KEY is unset (useful for tests / CI).
"""

from __future__ import annotations

from openai import OpenAI

from app.config import settings

_client: OpenAI | None = None


def get_client() -> OpenAI:
    global _client
    if _client is None:
        if not settings.openai_api_key:
            raise RuntimeError(
                "OPENAI_API_KEY is not set. Copy backend/.env.example to "
                "backend/.env and fill in your key."
            )
        _client = OpenAI(api_key=settings.openai_api_key)
    return _client
