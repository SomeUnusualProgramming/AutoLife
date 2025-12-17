# LifeAI Speech-to-Text Service (STT)

Offline, locally-running speech recognition microservice for the LifeAI application. Uses **Faster-Whisper** for CPU/GPU-optimized speech-to-text processing.

## Features

- ✅ **100% Offline** - No cloud API calls, no internet required
- ✅ **Fast & Lightweight** - Optimized for CPU with INT8 quantization
- ✅ **Multi-language** - Supports Polish and other languages
- ✅ **REST API** - Simple FastAPI endpoints
- ✅ **Logging & Error Handling** - Full observability
- ✅ **Health Check** - Built-in endpoint for monitoring

## Architecture

```
Frontend (React)
    ↓ Audio file (POST /api/speech-to-text/transcribe)
Spring Boot Backend
    ↓ Forward audio (POST http://localhost:5000/transcribe)
Python STT Microservice (Faster-Whisper)
    ↓ JSON response {text, language, confidence}
Spring Boot Backend
    ↓ Create Event & generate Recommendations
Database
```

## Quick Start

### Prerequisites

- Python 3.10+
- pip (Python package manager)
- ~1GB disk space (for Faster-Whisper model)
- CPU recommended (GPU optional)

### Installation

1. **Clone and navigate to the STT directory:**
   ```bash
   cd lifeai-stt
   ```

2. **Create virtual environment (recommended):**
   ```bash
   python -m venv venv
   
   # On Windows:
   venv\Scripts\activate
   
   # On macOS/Linux:
   source venv/bin/activate
   ```

3. **Install dependencies:**
   ```bash
   pip install -r requirements.txt
   ```

### Running the Service

**Development mode:**
```bash
python -m uvicorn app.main:app --host 0.0.0.0 --port 5000 --reload
```

**Production mode:**
```bash
python -m uvicorn app.main:app --host 0.0.0.0 --port 5000 --workers 1
```

**With Docker:**
```bash
docker build -t lifeai-stt .
docker run -p 5000:5000 lifeai-stt
```

## API Endpoints

### POST /transcribe

Transcribe audio file to text with automatic language detection.

**Request:**
```http
POST /transcribe
Content-Type: multipart/form-data

file: <audio_file.wav>
language: pl (optional)
```

**Response:**
```json
{
  "text": "Dziś jadłem obiad i poszedłem na spacer",
  "language": "pl",
  "confidence": 0.95,
  "processing_time_ms": 2340,
  "model": "faster-whisper"
}
```

**Supported audio formats:** WAV, MP3, M4A, OGG, WEBM

### GET /health

Health check endpoint to verify service availability.

**Response:**
```json
{
  "status": "healthy",
  "model": "faster-whisper",
  "version": "0.10.0"
}
```

## Configuration

Create a `.env` file in the `lifeai-stt` directory:

```
APP_NAME=LifeAI Speech-to-Text Service
APP_VERSION=1.0.0
DEBUG=False
WHISPER_MODEL_SIZE=base
LOG_LEVEL=INFO
```

**Model sizes:**
- `tiny` - Fastest, least accurate (~40MB)
- `base` - Recommended (default, ~140MB)
- `small` - Better accuracy (~465MB)
- `medium` - High accuracy (~1.5GB)
- `large` - Highest accuracy (~3GB)

## Backend Integration

The Spring Boot backend is configured to communicate with this STT service via:

**Configuration in `application.yml`:**
```yaml
stt:
  service-url: ${STT_SERVICE_URL:http://localhost:5000}
  default-language: pl
  connect-timeout: 30000
  read-timeout: 300000
```

**Endpoint:**
```java
POST /speech-to-text/transcribe
RequestParam: file (MultipartFile)
RequestParam: language (optional)
RequestParam: userId (optional)
```

When `userId` is provided, the backend automatically:
1. Transcribes the audio
2. Detects event type from transcribed text
3. Creates Event in database
4. Generates Recommendations

## Troubleshooting

**Model download fails:**
- Ensure internet connection for first run
- Models are cached in `~/.cache/huggingface`

**Slow processing:**
- Check CPU usage: `python app/services.py` takes ~2-5s first time
- For faster processing on repeated calls, model stays loaded

**Out of memory:**
- Use smaller model: `WHISPER_MODEL_SIZE=tiny`
- Reduce concurrent requests

## Logging

Logs are output to console with format:
```
2025-12-16 13:05:17,123 - app.services - INFO - Transcribing audio file: audio.wav
```

Check application logs for debugging.

## Performance Notes

- **First request:** ~5-10 seconds (model loading + processing)
- **Subsequent requests:** ~1-3 seconds (depends on audio length)
- **Memory:** ~300-500MB when running
- **CPU:** Single-threaded, uses 100% of one core during processing

## Future Enhancements

- [ ] Async processing queue
- [ ] Multiple language detection
- [ ] Real-time streaming transcription
- [ ] GPU support (CUDA/cuDNN)
- [ ] Batch processing API
- [ ] Custom model fine-tuning

## License

MIT
