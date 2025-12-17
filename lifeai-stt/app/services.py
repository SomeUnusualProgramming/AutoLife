import io
import logging
import time
import subprocess
import tempfile
from pathlib import Path
from typing import Optional, Tuple
from faster_whisper import WhisperModel

logger = logging.getLogger(__name__)


class WhisperService:
    def __init__(self, model_size: str = "base"):
        self.model_size = model_size
        self.model = None
        logger.info(f"Initializing Whisper service with model size: {model_size}")
        self._load_model()

    def _load_model(self):
        try:
            logger.info(f"Loading Faster-Whisper model: {self.model_size}")
            self.model = WhisperModel(self.model_size, device="cpu", compute_type="int8")
            logger.info("Model loaded successfully")
        except Exception as e:
            logger.error(f"Failed to load Whisper model: {e}")
            raise

    def _convert_webm_to_wav(self, audio_bytes: bytes) -> bytes:
        try:
            with tempfile.NamedTemporaryFile(suffix=".webm", delete=False) as webm_file:
                webm_path = webm_file.write(audio_bytes)
                webm_file.flush()
                webm_file_path = webm_file.name
            
            with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as wav_file:
                wav_file_path = wav_file.name
            
            try:
                cmd = [
                    "ffmpeg",
                    "-i", webm_file_path,
                    "-acodec", "pcm_s16le",
                    "-ar", "16000",
                    "-ac", "1",
                    "-y",
                    wav_file_path
                ]
                
                result = subprocess.run(cmd, capture_output=True, text=True, timeout=30)
                
                if result.returncode != 0:
                    logger.error(f"FFmpeg conversion error: {result.stderr}")
                    raise RuntimeError(f"Failed to convert WebM: {result.stderr}")
                
                with open(wav_file_path, 'rb') as f:
                    wav_bytes = f.read()
                
                logger.info(f"Converted WebM to WAV: {len(audio_bytes)} -> {len(wav_bytes)} bytes")
                return wav_bytes
                
            finally:
                Path(webm_file_path).unlink(missing_ok=True)
                Path(wav_file_path).unlink(missing_ok=True)
                
        except Exception as e:
            logger.error(f"WebM conversion failed: {e}")
            raise RuntimeError(f"Failed to convert audio format: {str(e)}")

    def _detect_audio_format(self, audio_bytes: bytes) -> str:
        if len(audio_bytes) < 12:
            return "unknown"
        
        if audio_bytes[:4] == b'RIFF' and audio_bytes[8:12] == b'WAVE':
            return "wav"
        elif audio_bytes[:3] == b'ID3' or audio_bytes[:2] == b'\xff\xfb':
            return "mp3"
        elif audio_bytes[:4] == b'\x1a\x45\xdf\xa3':
            return "webm"
        elif audio_bytes[:4] == b'\x00\x00\x00\x20':
            return "m4a"
        else:
            return "unknown"

    def transcribe(
        self, 
        audio_bytes: bytes, 
        language: Optional[str] = "pl"
    ) -> Tuple[str, Optional[str], float, int]:
        if self.model is None:
            raise RuntimeError("Whisper model is not loaded")
        
        if not audio_bytes or len(audio_bytes) == 0:
            raise RuntimeError("Audio data is empty")
        
        start_time = time.time()
        
        try:
            audio_format = self._detect_audio_format(audio_bytes)
            logger.info(f"Detected audio format: {audio_format}, size: {len(audio_bytes)} bytes")
            
            if audio_format == "webm":
                logger.info("Converting WebM to WAV for processing")
                audio_bytes = self._convert_webm_to_wav(audio_bytes)
            
            audio_stream = io.BytesIO(audio_bytes)
            
            logger.info(f"Starting transcription with language: {language}")
            
            segments, info = self.model.transcribe(
                audio_stream,
                language=language if language else None,
                beam_size=5,
            )
            
            full_text = "".join([segment.text for segment in segments]).strip()
            
            processing_time_ms = int((time.time() - start_time) * 1000)
            detected_language = info.language
            
            if not full_text:
                logger.warning(f"Empty transcription returned. Language: {detected_language}, Processing time: {processing_time_ms}ms")
            
            logger.info(
                f"Transcription completed. Text length: {len(full_text)}, "
                f"Detected language: {detected_language}, "
                f"Processing time: {processing_time_ms}ms"
            )
            
            return full_text, detected_language, 0.95, processing_time_ms
            
        except Exception as e:
            logger.error(f"Transcription error: {e}", exc_info=True)
            raise

    def is_healthy(self) -> bool:
        return self.model is not None
