import logging
from fastapi import FastAPI, UploadFile, File, Form, HTTPException
from fastapi.responses import JSONResponse
from contextlib import asynccontextmanager
import traceback
from typing import Optional

from app.config import settings, setup_logging
from app.services import WhisperService
from app.models import TranscriptionResponse, HealthResponse, ErrorResponse

logger = logging.getLogger(__name__)

whisper_service = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    global whisper_service
    setup_logging()
    logger.info(f"Starting {settings.app_name} v{settings.app_version}")
    
    try:
        whisper_service = WhisperService(model_size=settings.whisper_model_size)
        logger.info("Whisper service initialized successfully")
    except Exception as e:
        logger.error(f"Failed to initialize Whisper service: {e}")
        raise
    
    yield
    
    logger.info("Shutting down application")


app = FastAPI(
    title=settings.app_name,
    version=settings.app_version,
    lifespan=lifespan,
)


@app.get("/health", response_model=HealthResponse)
async def health_check():
    if whisper_service is None or not whisper_service.is_healthy():
        raise HTTPException(status_code=503, detail="Service unavailable")
    
    return HealthResponse(
        status="healthy",
        model="faster-whisper",
        version="0.10.0"
    )


@app.post("/transcribe", response_model=TranscriptionResponse)
async def transcribe_audio(
    file: UploadFile = File(...),
    language: Optional[str] = Form(default="pl"),
):
    if whisper_service is None or not whisper_service.is_healthy():
        logger.error("Whisper service is not initialized")
        raise HTTPException(status_code=503, detail="Service unavailable")
    
    if not file.filename:
        raise HTTPException(status_code=400, detail="No file provided")
    
    supported_formats = {"audio/wav", "audio/mpeg", "audio/mp4", "audio/ogg", "application/octet-stream"}
    if file.content_type not in supported_formats and not file.filename.endswith(('.wav', '.mp3', '.m4a', '.ogg', '.webm')):
        logger.warning(f"Unsupported file format: {file.content_type}")
        raise HTTPException(
            status_code=400,
            detail=f"Unsupported audio format: {file.content_type}. Supported: wav, mp3, m4a, ogg, webm"
        )
    
    try:
        audio_bytes = await file.read()
        file_size_mb = len(audio_bytes) / (1024 * 1024)
        
        logger.info(f"Processing audio file: {file.filename} ({file_size_mb:.2f} MB)")
        
        if not audio_bytes or len(audio_bytes) == 0:
            logger.error("Received empty audio file")
            raise HTTPException(
                status_code=400,
                detail="Audio file is empty"
            )
        
        text, detected_language, confidence, processing_time = whisper_service.transcribe(
            audio_bytes, 
            language=language
        )
        
        if not text or text.strip() == "":
            logger.warning(f"Empty transcription result for file: {file.filename}")
            raise HTTPException(
                status_code=422,
                detail=f"No speech detected in audio file. Please ensure the audio contains clear speech in the specified language ({language or 'auto-detect'})"
            )
        
        return TranscriptionResponse(
            text=text,
            language=detected_language or language,
            confidence=confidence,
            processing_time_ms=processing_time,
            model="faster-whisper"
        )
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Transcription error: {str(e)}\n{traceback.format_exc()}")
        raise HTTPException(
            status_code=500,
            detail=f"Transcription failed: {str(e)}"
        )


@app.exception_handler(Exception)
async def global_exception_handler(request, exc):
    logger.error(f"Unhandled exception: {str(exc)}\n{traceback.format_exc()}")
    return JSONResponse(
        status_code=500,
        content=ErrorResponse(
            error="Internal Server Error",
            detail=str(exc),
            code="INTERNAL_ERROR"
        ).dict()
    )


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "app.main:app",
        host="0.0.0.0",
        port=5000,
        reload=settings.debug,
        log_level=settings.log_level.lower()
    )
