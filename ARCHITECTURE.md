# LifeAI System Architecture

## Overview

LifeAI is a health and lifestyle management application built with a **microservices architecture**:

```
┌─────────────────────────────────────────────────────────────────┐
│                     Frontend (React)                             │
│  Audio recording, Event visualization, Recommendations display  │
└────────────────┬────────────────────────────────────────────────┘
                 │ HTTP/REST
                 ↓
┌─────────────────────────────────────────────────────────────────┐
│              Backend API (Spring Boot 3.2 Java 21)              │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ Controllers:                                             │  │
│  │ - SpeechToTextController (/speech-to-text/*)            │  │
│  │ - EventController (/events/*)                           │  │
│  │ - RecommendationController (/recommendations/*)         │  │
│  │ - TimelineController (/timeline/*)                      │  │
│  │ - UserController (/users/*)                             │  │
│  │ - AiController (/ai/*)                                  │  │
│  └──────────────────────────────────────────────────────────┘  │
│                         ↓                                        │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ Services:                                                │  │
│  │ - WhisperSpeechToTextService (→ STT Microservice)       │  │
│  │ - EventService                                           │  │
│  │ - RecommendationService + RuleEngine                    │  │
│  │ - TimelineService                                        │  │
│  │ - UserService                                            │  │
│  │ - CalendarService                                        │  │
│  │ - NotificationService                                    │  │
│  └──────────────────────────────────────────────────────────┘  │
│                         ↓                                        │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ Database (PostgreSQL):                                   │  │
│  │ - users, events, recommendations, timelines, calendars  │  │
│  │ - notifications, metadata                               │  │
│  └──────────────────────────────────────────────────────────┘  │
└────────────────┬────────────────────────────────────────────────┘
                 │ HTTP/REST (port 5000)
                 ↓
┌─────────────────────────────────────────────────────────────────┐
│     STT Microservice (Python FastAPI + Faster-Whisper)          │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ Endpoints:                                               │  │
│  │ POST /transcribe - Audio to Text                        │  │
│  │ GET /health - Service health check                      │  │
│  └──────────────────────────────────────────────────────────┘  │
│                         ↓                                        │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ Services:                                                │  │
│  │ - WhisperService (Faster-Whisper integration)           │  │
│  │ - Audio processing (WAV, MP3, M4A, OGG, WEBM)          │  │
│  │ - Language detection                                     │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                  │
│  100% Offline, CPU/GPU optimized, ~140MB model (base size)     │
└─────────────────────────────────────────────────────────────────┘
```

## Component Details

### 1. Frontend (React)

**Location:** `lifeai-frontend/`

**Responsibilities:**
- User interface for health event recording
- Audio capture and file upload
- Event visualization and timeline display
- Recommendation display and user actions
- User profile and settings management

**Key Technologies:**
- React 18+
- Vite (build tool)
- Tailwind CSS (styling)
- TypeScript

---

### 2. Backend (Spring Boot)

**Location:** `lifeai-backend/`

#### 2.1 Controllers

**SpeechToTextController** (`/speech-to-text/`)
- `POST /transcribe` - Accepts audio file, streams to STT microservice
- `POST /transcribe-bytes` - Accepts raw audio bytes
- Returns transcription + optionally creates Event + triggers Recommendations

**EventController** (`/events/`)
- CRUD operations for health events
- List events by date range, type
- Filter by user

**RecommendationController** (`/recommendations/`)
- Retrieve recommendations for user
- Mark recommendations as applied/planned
- Filter by priority and type

**TimelineController** (`/timeline/`)
- Get timeline view of events
- Grouped by date with event statistics

**UserController** (`/users/`)
- User registration and profile management
- User preferences

**HealthCheckController**
- Basic service health monitoring

#### 2.2 Services

**WhisperSpeechToTextService**
- Implements `SpeechToTextService` interface
- Communicates with Python STT microservice (localhost:5000)
- Handles audio file/bytes conversion
- Supports language parameter
- Timeout: 5 minutes (for long audio files)

**EventService**
- Create, read, update, delete events
- Filter by userId, type, date range
- Automatically create calendar entries for medical events
- Transaction management

**RecommendationService**
- Generate recommendations using rule engine
- Retrieve recommendations by various filters
- Mark recommendations as applied
- Integration with RecommendationRuleEngine

**RecommendationRuleEngine**
- Analyzes Event objects
- Applies business rules (e.g., "Food → suggest Physical Activity")
- Creates Recommendation entities
- Extensible rule system

**TimelineService**
- Aggregate events into timeline view
- Calculate statistics per day
- Format response for frontend

**CalendarService**
- Create calendar entries for important events
- Integration with event service

#### 2.3 Database Schema

**Users Table**
```sql
users (id, email, password, first_name, last_name, ...)
```

**Events Table**
```sql
events (id, user_id, event_type, description, timestamp, metadata JSON, ...)
```

**Recommendations Table**
```sql
recommendations (id, user_id, event_id, type, text, priority, is_applied, ...)
```

**Timelines Table**
```sql
timelines (id, user_id, event_id, date, ...)
```

**Calendars Table**
```sql
calendars (id, user_id, title, description, event_id, ...)
```

**Notifications Table**
```sql
notifications (id, user_id, type, title, message, ...)
```

---

### 3. STT Microservice (Python)

**Location:** `lifeai-stt/`

**Architecture:**
```
FastAPI App (port 5000)
    ↓
POST /transcribe Handler
    ↓
WhisperService
    ↓
Faster-Whisper Model (offline)
    ↓
Return JSON (text, language, confidence)
```

#### 3.1 Components

**main.py**
- FastAPI application definition
- Lifecycle management (model loading/unloading)
- Route definitions
- Error handling

**services.py - WhisperService**
- Manages Faster-Whisper model lifecycle
- Audio bytes → transcription
- Language detection
- Performance timing
- Health check

**models.py**
- Pydantic models for request/response validation
- `TranscriptionRequest`
- `TranscriptionResponse`
- `HealthResponse`
- `ErrorResponse`

**config.py**
- Settings management via `.env`
- Logging configuration
- Whisper model size selection

#### 3.2 API Contract

**Request Format:**
```
POST /transcribe
Content-Type: multipart/form-data

Parameters:
- file: binary audio file (required)
- language: "pl" | "en" | ... (optional, default="pl")
```

**Response Format:**
```json
{
  "text": "Transcribed audio text",
  "language": "pl",
  "confidence": 0.95,
  "processing_time_ms": 2340,
  "model": "faster-whisper"
}
```

#### 3.3 Technical Details

- **Framework:** FastAPI (async, lightweight)
- **Model:** Faster-Whisper (optimized implementation of OpenAI Whisper)
- **Device:** CPU (int8 quantization) or GPU (CUDA)
- **Supported Formats:** WAV, MP3, M4A, OGG, WEBM
- **Language Support:** 99+ languages (including Polish)
- **Model Sizes:**
  - tiny: 40MB, fastest
  - base: 140MB, recommended ✅
  - small: 465MB, better accuracy
  - medium: 1.5GB, high accuracy
  - large: 3GB, highest accuracy

---

## Data Flow: Audio to Recommendation

### Flow Diagram

```
1. Frontend captures audio
   └─→ User speaks: "Dzisiaj jadłem porcję makaronu"
       Format: WAV/MP3/M4A
       
2. Frontend sends to Backend
   └─→ POST /api/speech-to-text/transcribe
       Content: multipart/form-data (file, userId)
       
3. Backend receives audio
   └─→ SpeechToTextController.transcribeAudio()
       
4. Backend forwards to STT Microservice
   └─→ POST http://localhost:5000/transcribe
       Multipart: file, language="pl"
       Timeout: 5 minutes
       
5. STT Microservice processes
   └─→ WhisperService.transcribe()
       Faster-Whisper model runs
       Output: "Dzisiaj jadłem porcję makaronu"
       Processing time: 1-3 seconds
       
6. STT returns JSON response
   └─→ {
         "text": "Dzisiaj jadłem porcję makaronu",
         "language": "pl",
         "confidence": 0.95,
         "processing_time_ms": 2340
       }
       
7. Backend creates Event
   └─→ EventService.createEvent()
       Detect EventType: FOOD (from keywords)
       Save to database
       Created Event with ID = 123
       
8. Backend triggers Recommendations
   └─→ RecommendationService.generateRecommendationsForEvent()
       RuleEngine analyzes Event
       Rule: "FOOD → suggest PHYSICAL_ACTIVITY"
       Creates Recommendation:
         "Consider going for a walk to aid digestion"
       Saves to database
       
9. Backend returns complete response
   └─→ {
         "transcription": {
           "text": "Dzisiaj jadłem porcję makaronu",
           "language": "pl",
           "processing_time_ms": 2340
         },
         "event": {
           "id": 123,
           "type": "FOOD",
           "description": "Dzisiaj jadłem porcję makaronu",
           "timestamp": "2025-12-16T13:05:00"
         }
       }
       
10. Frontend receives response
    └─→ Display:
        ✓ Transcription: "Dzisiaj jadłem porcję makaronu"
        ✓ Event created (FOOD type)
        ✓ Recommendation: "Consider going for a walk..."
        ✓ Add to timeline
```

---

## Configuration Management

### Backend Configuration

**`application.yml`:**
```yaml
spring:
  application:
    name: lifeai-backend
  datasource:
    url: jdbc:postgresql://localhost:5432/lifeai
    username: postgres
    password: postgres
  jpa:
    hibernate:
      ddl-auto: validate

stt:
  service-url: ${STT_SERVICE_URL:http://localhost:5000}
  default-language: pl
  connect-timeout: 30000
  read-timeout: 300000

server:
  port: 8080

logging:
  level:
    com.lifeai: DEBUG
```

### STT Configuration

**`.env`:**
```
WHISPER_MODEL_SIZE=base
LOG_LEVEL=INFO
DEBUG=False
```

---

## Deployment

### Local Development

```bash
# Terminal 1: Start PostgreSQL
docker run --name postgres-lifeai -e POSTGRES_PASSWORD=postgres -p 5432:5432 postgres:15

# Terminal 2: Start STT Microservice
cd lifeai-stt
python -m uvicorn app.main:app --host 0.0.0.0 --port 5000

# Terminal 3: Start Backend
cd lifeai-backend
./mvnw spring-boot:run

# Terminal 4: Start Frontend
cd lifeai-frontend
npm run dev
```

### Docker Compose

```bash
docker-compose up
```

---

## Separation of Concerns

| Layer | Responsibility | Technology |
|-------|-----------------|------------|
| **Frontend** | UI, user interaction, visualization | React, TypeScript, Tailwind |
| **API** | REST endpoints, orchestration, business logic | Spring Boot, Java 21 |
| **Services** | Business logic, data processing | Spring Services, transactions |
| **STT** | Speech-to-text processing | Python, FastAPI, Faster-Whisper |
| **Database** | Persistent data storage | PostgreSQL |

---

## Extension Points

### Custom STT Service

To replace Faster-Whisper with another model:

1. Implement `SpeechToTextService` interface in Java
2. Update configuration to point to new service
3. No changes to controller or business logic required

**Example:**
```java
@Service
public class GoogleCloudSTTService implements SpeechToTextService {
    // Implementation using Google Cloud API
}
```

### Custom Recommendation Rules

Add new rules without modifying core engine:

```java
public class CustomRule implements RecommendationRule {
    @Override
    public List<Recommendation> apply(Event event) {
        // Custom logic
    }
}
```

---

## Security Considerations

- [ ] API authentication (JWT tokens)
- [ ] Input validation on all endpoints
- [ ] Audio file size limits
- [ ] Rate limiting per user
- [ ] HTTPS in production
- [ ] Database encryption at rest
- [ ] Audio file temporary storage cleanup

---

## Performance Optimization

- **Audio processing:** 1-3 seconds (CPU)
- **Event creation:** <100ms
- **Recommendation generation:** 50-200ms
- **Database queries:** <50ms (with proper indexing)

**Optimization opportunities:**
- Async task queue for recommendations
- Redis caching for recommendation rules
- Audio preprocessing before STT
- Model quantization for STT
- Database query optimization

---

## Monitoring & Logging

**Key metrics:**
- Audio transcription time
- Event creation success rate
- Recommendation generation latency
- API response times
- Database query performance

**Logs location:**
- Backend: `logs/lifeai-backend.log`
- STT: Console output (or file-based logging)

---

## Future Enhancements

1. **Real-time streaming transcription**
   - WebSocket connection for live audio
   - Partial transcriptions
   
2. **Offline mobile app**
   - React Native
   - Local database sync
   
3. **Advanced analytics**
   - Pattern detection in events
   - Health insights
   
4. **Multi-language support**
   - Automatic language switching
   - Translation service
   
5. **Voice command interface**
   - Direct voice control
   - Natural language understanding

---

## Glossary

| Term | Definition |
|------|-----------|
| **STT** | Speech-to-Text |
| **Event** | A recorded health/lifestyle event (food, activity, symptom) |
| **Recommendation** | Suggestion generated based on Event analysis |
| **Timeline** | Chronological view of user's events |
| **Rule Engine** | System for generating recommendations based on rules |
| **Microservice** | Independent service (STT) communicating via REST API |

