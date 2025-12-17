from pydantic import BaseModel, Field
from typing import Optional


class TranscriptionRequest(BaseModel):
    language: Optional[str] = Field(default="pl", description="Language code (e.g., 'pl', 'en')")


class TranscriptionResponse(BaseModel):
    text: str = Field(description="Transcribed text from audio")
    language: Optional[str] = Field(default="pl", description="Detected or specified language")
    confidence: Optional[float] = Field(default=None, description="Confidence score (0.0-1.0)")
    processing_time_ms: int = Field(description="Time taken to process audio in milliseconds")
    model: str = Field(default="faster-whisper", description="Model used for transcription")


class HealthResponse(BaseModel):
    status: str = Field(description="Health status: 'healthy' or 'unhealthy'")
    model: str = Field(description="Model name")
    version: str = Field(description="Faster-Whisper version")


class ErrorResponse(BaseModel):
    error: str = Field(description="Error message")
    detail: Optional[str] = Field(default=None, description="Additional error details")
    code: Optional[str] = Field(default=None, description="Error code")
