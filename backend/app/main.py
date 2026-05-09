"""FastAPI app entry point.

Run locally with:

    cd backend
    uvicorn app.main:app --reload --port 8000

Then expose via ngrok for the demo:

    ngrok http 8000
"""

from __future__ import annotations

import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI

from app.api.gateway import router, set_dlcs
from app.config import settings
from app.db_service.session import init_db
from app.game_logic.content_loader import load_all_dlcs
from app.rag_service.knowledge_loader import init_chroma

logging.basicConfig(level=settings.log_level)
log = logging.getLogger("hp.main")


@asynccontextmanager
async def lifespan(app: FastAPI):
    log.info("Initializing DB at %s", settings.sqlite_path)
    init_db()

    log.info("Loading DLC content from %s", settings.dlc_data_dir)
    dlcs = load_all_dlcs(settings.dlc_data_dir)
    set_dlcs(dlcs)
    log.info("Loaded %d DLC(s): %s", len(dlcs), list(dlcs.keys()))

    all_lore: list[dict] = []
    for c in dlcs.values():
        all_lore.extend(c.lore)
    init_chroma(all_lore)

    yield
    log.info("Shutdown.")


app = FastAPI(
    title="HP Project Backend",
    version="0.1.0",
    description="Trapped in Harry Potter: Rewrite the Fate — backend.",
    lifespan=lifespan,
)
app.include_router(router)


@app.get("/healthz", tags=["meta"])
def healthz() -> dict[str, str]:
    return {"status": "ok"}
