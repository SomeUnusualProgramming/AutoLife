import logging
from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    app_name: str = "LifeAI Speech-to-Text Service"
    app_version: str = "1.0.0"
    debug: bool = False
    whisper_model_size: str = "base"
    log_level: str = "INFO"
    
    class Config:
        env_file = ".env"
        case_sensitive = False


settings = Settings()


def setup_logging():
    logging.basicConfig(
        level=settings.log_level,
        format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
    )
