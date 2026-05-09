from __future__ import annotations

from types import SimpleNamespace

import pytest

from app.rag_service import knowledge_loader as kl


class FakeEmbeddingsClient:
    def __init__(self) -> None:
        self.calls: list[dict[str, object]] = []

    def create(self, input: list[str], model: str):
        self.calls.append({"input": input, "model": model})
        return SimpleNamespace(
            data=[
                SimpleNamespace(index=index, embedding=[float(index), float(len(text))])
                for index, text in reversed(list(enumerate(input)))
            ]
        )


class FakeOpenAI:
    embeddings_client = FakeEmbeddingsClient()

    def __init__(self, api_key: str) -> None:
        self.api_key = api_key
        self.embeddings = self.embeddings_client


@pytest.fixture(autouse=True)
def reset_chroma_state(monkeypatch, tmp_path):
    monkeypatch.setattr(kl, "_client", None)
    monkeypatch.setattr(kl, "_collection", None)
    monkeypatch.setattr(kl.settings, "openai_api_key", "")
    monkeypatch.setattr(kl.settings, "openai_embedding_model", "text-embedding-3-small")
    monkeypatch.setattr(kl.settings, "chroma_path", tmp_path / "chroma")

    FakeOpenAI.embeddings_client = FakeEmbeddingsClient()
    monkeypatch.setattr(kl, "OpenAI", FakeOpenAI)

    yield

    monkeypatch.setattr(kl, "_client", None)
    monkeypatch.setattr(kl, "_collection", None)


def test_openai_embedding_function_uses_v1_client_and_sorts_embeddings():
    embedding_fn = kl.OpenAIEmbeddingFunction(
        api_key="sk-test",
        model_name="text-embedding-3-small",
    )

    embeddings = embedding_fn(["first\nline", "second"])

    assert FakeOpenAI.embeddings_client.calls == [
        {
            "input": ["first line", "second"],
            "model": "text-embedding-3-small",
        }
    ]
    assert [embedding.tolist() for embedding in embeddings] == [
        [0.0, 10.0],
        [1.0, 6.0],
    ]


def test_init_chroma_skips_bootstrap_without_openai_key(caplog):
    kl.init_chroma(
        [
            {
                "id": "veil",
                "title": "The Veil",
                "text": "An ancient archway.",
                "tags": ["mystery"],
            }
        ]
    )

    assert kl._client is None
    assert kl._collection is None
    assert "OPENAI_API_KEY not set" in caplog.text

    with pytest.raises(RuntimeError, match="ChromaDB not initialized"):
        kl.get_collection()


def test_init_chroma_seeds_only_missing_lore_entries():
    kl.settings.openai_api_key = "sk-test"
    lore_entries = [
        {
            "id": "veil",
            "title": "The Veil",
            "text": "An ancient archway.",
            "tags": ["mystery", "death"],
        },
        {
            "id": "sirius",
            "title": "Sirius Black",
            "text": "Harry's godfather.",
            "tags": ["person"],
        },
    ]

    kl.init_chroma(lore_entries)
    collection = kl.get_collection()

    assert collection.count() == 2
    seeded = collection.get(ids=["veil", "sirius"])
    assert seeded["ids"] == ["veil", "sirius"]
    assert seeded["documents"] == ["An ancient archway.", "Harry's godfather."]
    assert seeded["metadatas"] == [
        {"title": "The Veil", "tags": "mystery,death"},
        {"title": "Sirius Black", "tags": "person"},
    ]

    kl.init_chroma(
        lore_entries
        + [
            {
                "id": "prophecy",
                "title": "The Prophecy",
                "text": "Neither can live while the other survives.",
                "tags": [],
            }
        ]
    )

    assert kl.get_collection().count() == 3
    assert kl.get_collection().get(ids=["prophecy"])["documents"] == [
        "Neither can live while the other survives."
    ]
