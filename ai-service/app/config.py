from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """
    Phase 0 foundation settings only. Retrieval/generation configuration
    (vector store, LLM provider, context limits) is added in Phase 11
    (AI/RAG) per the HELIOS implementation order.
    """

    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    environment: str = "development"
    ai_service_port: int = 8000
    backend_base_url: str = "http://localhost:8080/api/v1"


settings = Settings()
