"""
Centralized config. Reads from environment / .env file.

Keep `openai_api_key` optional at config-load time so the FastAPI app can
import and start without a real key (useful for unit tests, IDE inspection,
and CI). The first runtime call into OpenAI / ChromaDB will fail loudly if
the key is missing.
"""

from __future__ import annotations

from pathlib import Path

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict

BACKEND_ROOT = Path(__file__).resolve().parents[1]
PROJECT_ROOT = BACKEND_ROOT.parent
DEFAULT_DLC_DATA_DIR = PROJECT_ROOT / "data" / "dlcs"


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=BACKEND_ROOT / ".env",
        env_file_encoding="utf-8",
        extra="ignore",
        case_sensitive=False,
    )

    openai_api_key: str = Field(default="", alias="OPENAI_API_KEY")
    openai_chat_model: str = Field(default="gpt-4o-mini")
    openai_embedding_model: str = Field(default="text-embedding-3-small")

    sqlite_path: Path = BACKEND_ROOT / "hp.db"
    chroma_path: Path = BACKEND_ROOT / "chroma_db"
    dlc_data_dir: Path = DEFAULT_DLC_DATA_DIR

    log_level: str = "INFO"


settings = Settings()
