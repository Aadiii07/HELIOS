from fastapi import FastAPI

from app.config import settings

app = FastAPI(
    title="HELIOS AI Service",
    description="RAG/AI assistant service for HELIOS. Phase 0: foundation only.",
    version="0.1.0",
)


@app.get("/health")
def health() -> dict:
    return {"status": "ok", "environment": settings.environment}
