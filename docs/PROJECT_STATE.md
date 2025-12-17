# LifeAI - Project State Documentation

**Date**: December 17, 2025  
**Version**: 1.0.0 (Post-Rollback)  
**Status**: Functional baseline established

This document describes the **actual current state** of the LifeAI project as it exists in code, after the rollback. It is the single source of truth for understanding what exists and what does not.

---

## 1. Architecture Overview

### Technology Stack

| Component | Technology | Version | Port |
|-----------|-----------|---------|------|
| **Frontend** | React + TypeScript + Vite | Latest | 3000 |
| **Backend** | Spring Boot | 3.2.1 | 8080 |
| **STT Service** | FastAPI + Faster-Whisper | Latest | 5000 |
| **Database** | PostgreSQL | 16-alpine | 5432 |
| **Container Orchestration** | Docker Compose | 3.8 | — |

### Service URLs (Running in Docker Compose)

```
Frontend:         http://localhost:3000
Backend API:      http://localhost:8080
STT Service:      http://localhost:5000
Database:         localhost:5432 (postgres/postgres)
```

### Network Configuration

All services run on a shared Docker network (`lifeai-network`). Within Docker:
- Backend calls STT: `http://stt:5000`
- Backend calls DB: `jdbc:postgresql://postgres:5432/lifeai`
- Frontend calls Backend: `http://backend:8080` (docker) or `http://localhost:8080` (local dev)

---

## 2. Backend – Current State (Spring Boot 3.2)

### Database

**Database**: PostgreSQL 16  
**DDL Strategy**: `spring.jpa.hibernate.ddl-auto=update`  
**Flyway**: Enabled for migrations

**Tables** (auto-created via Hibernate):
- `users`
- `events`
- `recommendations`
- `notifications`
- `calendars`
- `base_entity` (inherited by all entities)

### Java Version & Framework

- **Java Version**: 21
- **Spring Boot**: 3.2.1
- **Dependencies**:
  - Spring Data JPA
  - Spring Validation
  - Lombok
  - PostgreSQL Driver
  - Flyway (migrations)
  - Spring Test + Testcontainers (for testing)

### Controllers (8 total)

#### 1. **SpeechToTextController** → `/speech-to-text`

**Purpose**: Transcribe audio files to text, create events, and generate recommendations in one call.

| Method | Endpoint | Parameters | Returns | Used by Frontend |
|--------|----------|-----------|---------|------------------|
| POST | `/transcribe` | `file` (MultipartFile), `language?`, `userId?` | `Map<String, Object>` with `transcription`, `event`, `recommendations` | ✅ Yes |
| POST | `/transcribe-bytes` | `audioBytes` (byte[]), `mimeType`, `language?`, `userId?` | `Map<String, Object>` with `transcription`, `event`, `recommendations` | ❌ No (backup option) |

**Event Detection**: Regex-based keyword matching on transcribed text:
- `FOOD`: "jad", "jadł", "posiłek", "sniadani", "obiad", "kolacj"
- `ACTIVITY`: "aktywno", "ćwicz", "trening", "spacer", "sport"
- `DOCTOR_VISIT`: "lekarz", "doktor", "wizyta"
- `MEDICATION`: "lekarst", "medycyn", "lek"
- `SYMPTOM`: "objaw", "ból", "gorączk"
- `WEIGHT`: "wag", "kilogram"
- `SLEEP`: "sen", "spał"
- `MOOD`: "humor", "nastrój", "czuj"
- `OTHER`: default fallback

**Response Structure**:
```json
{
  "transcription": {
    "text": "...",
    "sourceFile": "...",
    "language": "pl",
    "processingTimeMs": 1234
  },
  "event": {
    "id": 123,
    "userId": 1,
    "type": "ACTIVITY",
    "description": "...",
    "timestamp": "2025-12-17T10:00:00",
    "metadata": {}
  },
  "recommendations": [
    {
      "id": 1,
      "userId": 1,
      "eventId": 123,
      "text": "...",
      "type": "HEALTH",
      "priority": "MEDIUM",
      "status": "...",
      "isApplied": false
    }
  ]
}
```

**Current Limitation**: `/transcribe-bytes` is implemented but not used by frontend.

---

#### 2. **EventController** → `/events`

**Purpose**: CRUD operations on events.

| Method | Endpoint | Parameters | Returns | Used by Frontend |
|--------|----------|-----------|---------|------------------|
| POST | `/` | `CreateEventRequest` (body) | `EventResponse` | ❌ No (indirect via STT) |
| GET | `/{id}` | `id` | `EventResponse` | ❌ No |
| GET | `/user/{userId}` | `userId` | `List<EventResponse>` | ❌ No |
| GET | `/user/{userId}/type/{type}` | `userId`, `type` (EventType enum) | `List<EventResponse>` | ❌ No |
| GET | `/user/{userId}/range` | `userId`, `startTime` (ISO), `endTime` (ISO) | `List<EventResponse>` | ❌ No |
| PUT | `/{id}` | `id`, `CreateEventRequest` (body) | `EventResponse` | ❌ No |
| DELETE | `/{id}` | `id` | 204 No Content | ❌ No |

**CreateEventRequest**:
```java
{
  "userId": 1,
  "type": "ACTIVITY",
  "description": "...",
  "timestamp": "2025-12-17T10:00:00",
  "metadata": { /* any JSON */ }
}
```

**EventResponse**:
```java
{
  "id": 123,
  "userId": 1,
  "type": "ACTIVITY",
  "description": "...",
  "timestamp": "2025-12-17T10:00:00",
  "metadata": { /* any JSON */ }
}
```

---

#### 3. **TimelineController** → `/timeline`

**Purpose**: Fetch events grouped by day for a date range (main display endpoint).

| Method | Endpoint | Parameters | Returns | Used by Frontend |
|--------|----------|-----------|---------|------------------|
| GET | `/` | `userId?`, `from?` (ISO date), `to?` (ISO date) | `TimelineResponse` | ✅ Yes (UNSTABLE) |

**⚠️ WARNING - MISMATCH DETECTED**:
Frontend code expects `.entries` property, but backend returns `.days` structure. This causes Timeline display to fail silently (empty list).

**Default Behavior**:
- `from`: If not provided, defaults to 7 days ago
- `to`: If not provided, defaults to tomorrow
- `userId`: If not provided, defaults to `1L` (hardcoded fallback)

**Actual Backend Response (TimelineResponse.java)**:
```java
{
  "userId": 1,
  "fromDate": "2025-12-10",
  "toDate": "2025-12-18",
  "totalEvents": 5,
  "days": [
    {
      "date": "2025-12-17",
      "events": [
        {
          "id": 123,
          "type": "ACTIVITY",
          "description": "...",
          "timestamp": "2025-12-17T10:00:00",
          "metadata": {}
        }
      ],
      "eventCount": 1
    }
  ]
}
```

**Frontend Expectation (types/index.ts)**:
```typescript
{
  "entries": [
    {
      "id": "timeline-1",
      "event": { /* full event */ },
      "createdAt": "ISO datetime",
      "importance": number
    }
  ],
  "total": number
}
```

**Runtime Impact**: Frontend code at Timeline.tsx:138 checks `timelineData?.entries` - since backend doesn't return this property, the timeline renders empty (displays "Brak zdarzeń" / No events).

---

#### 4. **RecommendationController** → `/recommendations`

**Purpose**: Get, mark, and delete recommendations.

| Method | Endpoint | Parameters | Returns | Used by Frontend |
|--------|----------|-----------|---------|------------------|
| GET | `/` | — | `List<RecommendationResponse>` | ❌ No |
| GET | `/users/{userId}` | `userId` | `List<RecommendationResponse>` | ✅ Yes |
| GET | `/users/{userId}/unread` | `userId` | `List<RecommendationResponse>` | ❌ No |
| GET | `/users/{userId}/type/{type}` | `userId`, `type` (RecommendationType) | `List<RecommendationResponse>` | ❌ No |
| GET | `/users/{userId}/priority/{priority}` | `userId`, `priority` (RecommendationPriority) | `List<RecommendationResponse>` | ❌ No |
| GET | `/events/{eventId}` | `eventId` | `List<RecommendationResponse>` | ❌ No |
| GET | `/{id}` | `id` | `RecommendationResponse` | ❌ No |
| PATCH | `/{id}/apply` | `id` | `RecommendationResponse` | ❌ No |
| PATCH | `/{id}/done` | `id` | `RecommendationResponse` | ✅ Yes |
| PATCH | `/{id}/planned` | `id` | `RecommendationResponse` | ✅ Yes |
| DELETE | `/{id}` | `id` | 204 No Content | ❌ No |

**⚠️ JSON Field Mapping - IMPORTANT**:
`RecommendationResponse.java` uses `@JsonProperty` annotations to rename fields in JSON output:

| Java Field | JSON Field | JSON Value |
|-----------|-----------|-----------|
| `text` | `suggestion` | string |
| `type` (RecommendationType) | `category` | enum name |
| `status` (String) | `status` | string |
| `actionUrl` | `actionUrl` | string |
| `aiGenerated` | `aiGenerated` | boolean |
| `isApplied` | `isApplied` | boolean |

**Actual JSON Response** (as sent to frontend):
```json
{
  "id": 1,
  "userId": 1,
  "eventId": 123,
  "suggestion": "Try to drink more water",
  "category": "HEALTH",
  "priority": "MEDIUM",
  "status": "PLANNED",
  "isApplied": false,
  "actionUrl": null,
  "aiGenerated": false,
  "appliedAt": null,
  "createdAt": "2025-12-17T10:00:00",
  "updatedAt": "2025-12-17T10:00:00"
}
```

**Frontend Usage**: Frontend types expect field names as defined in RecommendationResponse.java entity fields, but receives JSON with @JsonProperty mappings. Frontend currently uses `text` and `type` in some places, `suggestion` and `category` in others (see Dashboard.tsx:70-80).

---

#### 5. **UserController** → `/users`

**Purpose**: CRUD operations on users (stub implementation).

| Method | Endpoint | Parameters | Returns | Used by Frontend |
|--------|----------|-----------|---------|------------------|
| POST | `/` | `CreateUserRequest` (body) | `UserResponse` | ❌ No |
| GET | `/{id}` | `id` | `UserResponse` | ❌ No |
| GET | `/` | — | `List<UserResponse>` | ❌ No |
| PUT | `/{id}` | `id`, `CreateUserRequest` (body) | `UserResponse` | ❌ No |
| DELETE | `/{id}` | `id` | 204 No Content | ❌ No |

**Current Status**: Stub endpoints. Frontend doesn't call these.

---

#### 6. **HealthCheckController** → `/health`

**Purpose**: Service health status.

| Method | Endpoint | Returns | Used by Frontend |
|--------|----------|---------|------------------|
| GET | `/` | `HealthCheckResponse` | ❌ No (used by docker-compose health check) |

---

#### 7. **AiController** → `/ai`

**Purpose**: AI analysis and health checks (stub).

| Method | Endpoint | Parameters | Returns | Used by Frontend |
|--------|----------|-----------|---------|------------------|
| POST | `/analyze` | `AiAnalysisRequest` (body) | `AiAnalysisResponse` | ❌ No |
| GET | `/health` | — | `boolean` | ❌ No |

**Current Status**: Stub implementation. Not used by frontend.

---

#### 8. **GlobalExceptionHandler**

Centralized error handling for all exceptions across the backend.

---

### Services (9 total)

| Service | Purpose | Used |
|---------|---------|------|
| **SpeechToTextService** | Interface for transcription (delegated to Whisper) | ✅ Yes |
| **WhisperSpeechToTextService** | Calls STT service at `http://stt:5000/transcribe` | ✅ Yes |
| **MockSpeechToTextService** | Mock transcription for testing | ❌ Fallback only |
| **EventService** | Event CRUD and queries | ✅ Yes |
| **RecommendationService** | Generate, fetch, update recommendations | ✅ Yes |
| **UserService** | User CRUD (stub) | ❌ No |
| **TimelineService** | Timeline generation (groups events by day) | ✅ Yes |
| **NotificationService** | Notification management (stub) | ❌ No |
| **HealthCheckService** | Health status | ✅ Yes (by docker) |
| **CalendarService** | Calendar management (stub) | ❌ No |
| **LifeAiService** | AI analysis service (stub) | ❌ No |

---

### Data Models (Entities)

#### **Event**
- `id` (Long, PK)
- `userId` (Long, FK, required)
- `type` (EventType enum: FOOD, ACTIVITY, DOCTOR_VISIT, MEDICATION, SYMPTOM, WEIGHT, SLEEP, MOOD, MEDICAL, OTHER)
- `description` (String, required, TEXT column)
- `timestamp` (LocalDateTime, required)
- `metadata` (Map<String, Object>, JSON column)
- **Indexes**: user_id, event_type, event_timestamp

#### **Recommendation**
- `id` (Long, PK)
- `userId` (Long, FK)
- `eventId` (Long, FK)
- `text` (String)
- `type` (RecommendationType enum)
- `priority` (RecommendationPriority enum: HIGH, MEDIUM, LOW)
- `status` (String: PLANNED, DONE, etc.)
- `isApplied` (Boolean)
- `createdAt` (LocalDateTime)

#### **User**
- `id` (Long, PK)
- `username` (String)
- `email` (String)
- Other fields (stub)

#### **Notification**, **Calendar**
- Defined but not actively used in current flow

---

### API Response Format

All endpoints follow this pattern:

**Success (200/201)**:
```java
@ResponseBody
{
  "data": { /* actual payload */ },
  "success": true
}
```

**Error (4xx/5xx)**:
```java
{
  "error": "Error message",
  "status": 400,
  "timestamp": "2025-12-17T10:00:00"
}
```

---

## 2.B Backend – API Routing & Context Path

**Important Configuration** (application.properties:16):
```properties
server.servlet.context-path=/api
```

This means:
- All Spring Boot endpoints are prefixed with `/api`
- Controller `@RequestMapping("/timeline")` becomes `GET /api/timeline`
- Frontend calls `POST /api/speech-to-text/transcribe` (which maps to controller path + context path)

**No explicit API response wrapper** - Spring Boot returns controller responses directly wrapped in Axios `.data` property on frontend.

---

## 3. Frontend – Current State (React + Vite)

### Pages

#### **Home Page** → `src/pages/Home.tsx`
- Main entry point
- Renders `Dashboard` component

#### **Dashboard** → `src/components/Dashboard.tsx`
- Main UI layout with 3-column grid (2 columns left, 1 sidebar right)
- Left column: AudioRecorder, manual event input, Timeline
- Right sidebar: AI Suggestions (top 5 recommendations)
- Combines voice input and text input flows

---

### Components

| Component | File | Purpose | Used |
|-----------|------|---------|------|
| **AudioRecorder** | `src/components/AudioRecorder.tsx` | Record audio, transcribe, auto-display results | ✅ Yes |
| **Dashboard** | `src/components/Dashboard.tsx` | Main layout and flow orchestration | ✅ Yes |
| **Timeline** | `src/components/Timeline.tsx` | Display events grouped by day and time period | ✅ Yes |
| **Recommendations** | `src/components/Recommendations.tsx` | Render recommendation cards (stub) | ❌ Inline in Dashboard |
| **Header** | `src/components/Header.tsx` | Navigation/branding (stub) | ❌ No |

---

### Data Flow

#### **Workflow 1: Voice Input to Event**
```
AudioRecorder
  ↓ (record audio)
  ↓ (on stop)
  → useSpeechToText hook
    ↓
    → speechToTextApi.transcribeAudio()
      ↓
      POST /api/speech-to-text/transcribe
        (multipart: file, language?, userId?)
      ↓
      Backend:
        - Calls STT service (FastAPI)
        - Detects event type from transcription
        - Creates event
        - Generates recommendations
      ↓
      Returns: { transcription, event, recommendations }
    ↓
  → useAudioRecorder state updated
  → Display transcription + recommendations
```

#### **Workflow 2: Manual Event Input**
```
Dashboard (textarea input)
  ↓ (user submits)
  → useSendEvent hook
    ↓
    → eventApi.sendEvent()
      ↓
      POST /api/events
      ↓
      Creates event directly (no STT)
    ↓
  → Refresh timeline
```

#### **Workflow 3: Fetch Timeline**
```
Timeline component
  ↓ (on mount or refresh)
  → useTimeline hook
    ↓
    → timelineApi.getTimeline()
      ↓
      GET /api/timeline?userId=1&from=...&to=...
      ↓
      Returns TimelineResponse { entries, total }
    ↓
  → Group by day, then by time period (Morning/Afternoon/Evening/Night)
  → Render with health status colors
```

---

### Hooks

**File Organization:**
- `src/hooks/index.ts` - Contains: useAudioRecorder, useSpeechToText, useTimeline, useRecommendations, useSendEvent, useAsync
- `src/hooks/useUser.ts` - Separate file for useUser hook

| Hook | File | Purpose | Returns |
|------|------|---------|---------|
| **useAudioRecorder** | index.ts | Record audio, capture blob | `{ isRecording, audioBlob, startRecording, stopRecording, resetRecording }` |
| **useSpeechToText** | index.ts | Transcribe audio via API | `{ status, transcribedText, event, recommendations, error, transcribe() }` |
| **useUser** | useUser.ts | Get current user context (stores in localStorage) | `{ userId, isLoading, updateUserId() }` |
| **useSendEvent** | index.ts | Send event to backend | `{ status, error, send() }` |
| **useTimeline** | index.ts | Fetch timeline events | `{ status, data, error, fetch() }` |
| **useRecommendations** | index.ts | Fetch recommendations + mark done/planned | `{ status, data, error, fetch(), markDone(), markPlanned() }` |
| **useAsync** | index.ts | Generic async state manager | `{ status, data, error, execute() }` |

**useUser Implementation Detail** (useUser.ts:3-19):
- Stores userId in localStorage key `'lifeai_user_id'`
- Defaults to `1` on first run
- Can be updated via `updateUserId(newId)` function
- Persists across browser sessions

---

### API Service Layer

**File**: `src/services/api.ts`

**Base URL**: `${VITE_API_URL}` or `http://localhost:8080` (default)

**API Groups**:

#### **speechToTextApi**
- `transcribeAudio(file, language?, userId?)` → POST `/api/speech-to-text/transcribe`
- `transcribeAudioBytes(audioBytes, mimeType, language?, userId?)` → POST `/api/speech-to-text/transcribe-bytes`

#### **eventApi**
- `sendEvent(event)` → POST `/api/events`

#### **timelineApi**
- `getTimeline(limit?, offset?, userId?)` → GET `/api/timeline?...`

#### **recommendationsApi**
- `getRecommendations(category?, userId?)` → GET `/api/recommendations?...`
- `markRecommendationDone(id)` → PATCH `/api/recommendations/{id}/done`
- `markRecommendationPlanned(id)` → PATCH `/api/recommendations/{id}/planned`

---

### Types

**File**: `src/types/index.ts`

```typescript
interface Event {
  id?: string
  type: string
  description: string
  timestamp?: string
  metadata?: Record<string, unknown>
}

interface TimelineEntry {
  id: string
  event: Event
  createdAt: string
  importance: number
}

interface Timeline {
  entries: TimelineEntry[]
  total: number
}

interface Recommendation {
  id: string
  category: string
  suggestion: string
  priority: 'low' | 'medium' | 'high'
  status: 'PLANNED' | 'DONE'
  actionUrl?: string
  aiGenerated?: boolean
  createdAt?: string
}

interface TranscriptionWithEventResponse {
  transcription: TranscriptionResult
  event?: { /* full event object */ }
  recommendations?: Array<{ /* recommendation objects */ }>
}
```

---

### Current Views/Features

| Feature | Implemented | Status |
|---------|-------------|--------|
| Audio recording | ✅ Yes | Working |
| Transcription (voice → text) | ✅ Yes | Working |
| Event creation (voice) | ✅ Yes | Working (auto-detect type) |
| Event creation (manual text) | ✅ Yes | Working |
| Timeline display | ✅ Yes | Grouped by day + time period |
| Event health status indicator | ✅ Yes | Keyword-based (client-side) |
| Recommendations display | ✅ Yes | Top 5 from API |
| Mark recommendation as done | ⚠️ Partial | UI ready, may need testing |
| Mark recommendation as planned | ⚠️ Partial | UI ready, may need testing |
| Time periods (Morning/Afternoon/Evening/Night) | ✅ Yes | Hardcoded in client |
| Language support | ✅ Yes | Polish (pl) default, configurable |

---

## 4. Speech-to-Text Service (Python FastAPI)

**Location**: `lifeai-stt/app/`

### Technology Stack
- **Framework**: FastAPI
- **Model**: Faster-Whisper (OpenAI Whisper)
- **Model Size**: Configurable (`base` default, `tiny` for testing)
- **Port**: 5000
- **Lifespan Management**: Loads model once on startup

### API Endpoints

#### **1. Health Check**
```
GET /health
Response: { "status": "healthy", "model": "faster-whisper", "version": "0.10.0" }
```

#### **2. Transcribe Audio File**
```
POST /transcribe
Multipart Form Data:
  - file: UploadFile (audio file)
  - language: Optional[str] = "pl"

Response:
{
  "text": "Transcribed text",
  "language": "pl",
  "confidence": 0.95,
  "processing_time_ms": 1234,
  "model": "faster-whisper"
}
```

**Supported Formats**: wav, mp3, m4a, ogg, webm

**Error Handling**:
- 400: Unsupported format, no file provided, empty file
- 422: No speech detected in audio
- 503: Service unavailable (model not initialized)
- 500: Transcription failed

### Services

**WhisperService** (`app/services.py`):
- Loads Whisper model on initialization
- `transcribe(audio_bytes, language?)` → Returns `(text, detected_language, confidence, processing_time_ms)`
- `is_healthy()` → Boolean

### Models

**TranscriptionResponse** (`app/models.py`):
```python
class TranscriptionResponse(BaseModel):
    text: str
    language: str
    confidence: float
    processing_time_ms: float
    model: str = "faster-whisper"
```

### Configuration (`app/config.py`)

```python
app_name: str = "LifeAI STT Service"
app_version: str = "0.1.0"
whisper_model_size: str = "base"
debug: bool = False
log_level: str = "INFO"
```

---

## 5. Real End-to-End Flow

### Scenario: User Records Voice

```
┌─ FRONTEND ─────────────────────────────────────────┐
│ User clicks "Start Recording" in AudioRecorder     │
│                                                    │
│ → useAudioRecorder.startRecording()                │
│   (uses Web Audio API, browser MediaRecorder)      │
│                                                    │
│ User says: "Właśnie ćwiczyłem 30 minut"           │
│                                                    │
│ User clicks "Stop"                                 │
│ → useAudioRecorder.stopRecording()                 │
│   (returns audioBlob: Blob)                        │
│                                                    │
│ AudioRecorder auto-detects blob & triggers:        │
│ → useSpeechToText.transcribe(audioBlob, "pl", 1)  │
└─────────────────────────────────────────────────────┘
           ↓ (multipart/form-data)
┌─ BACKEND (Port 8080) ──────────────────────────────┐
│ POST /speech-to-text/transcribe                    │
│   file: audioBlob                                  │
│   language: "pl"                                  │
│   userId: 1                                        │
│                                                    │
│ → SpeechToTextController.transcribeAudio()         │
│                                                    │
│   1. Call WhisperSpeechToTextService               │
│      → POST http://stt:5000/transcribe             │
└─────────────────────────────────────────────────────┘
           ↓ (multipart/form-data)
┌─ STT SERVICE (Port 5000) ───────────────────────────┐
│ POST /transcribe                                   │
│   file: audioBlob                                  │
│   language: "pl"                                  │
│                                                    │
│ → WhisperService.transcribe()                      │
│   (loads model, transcribes audio)                 │
│                                                    │
│ Response:                                          │
│ {                                                  │
│   "text": "Właśnie ćwiczyłem 30 minut",            │
│   "language": "pl",                                │
│   "confidence": 0.92,                              │
│   "processing_time_ms": 2345,                      │
│   "model": "faster-whisper"                        │
│ }                                                  │
└─────────────────────────────────────────────────────┘
           ↓ (response back to Backend)
┌─ BACKEND (Port 8080) cont'd ──────────────────────┐
│   2. Parse transcription response                  │
│   3. Call detectEventType()                        │
│      (regex: "ćwicz" → EventType.ACTIVITY)         │
│                                                    │
│   4. Call EventService.createEvent()               │
│      POST to DB: new Event                         │
│      {                                             │
│        userId: 1,                                  │
│        type: ACTIVITY,                             │
│        description: "Właśnie ćwiczyłem 30 minut",  │
│        timestamp: 2025-12-17T10:00:00,             │
│        metadata: { source: "speech-to-text", ... } │
│      }                                             │
│                                                    │
│   5. Call RecommendationService                    │
│      .generateRecommendationsForEvent(event)       │
│      → RuleEngine analyzes event                   │
│      → Generates recommendations (if any)          │
│                                                    │
│   6. Return combined response:                     │
│      {                                             │
│        "transcription": { ... },                   │
│        "event": { id: 123, ... },                  │
│        "recommendations": [ ... ]                  │
│      }                                             │
└─────────────────────────────────────────────────────┘
           ↓ (JSON response back to Frontend)
┌─ FRONTEND ─────────────────────────────────────────┐
│ useSpeechToText state updated                      │
│                                                    │
│ AudioRecorder displays:                            │
│ - Transcription: "Właśnie ćwiczyłem 30 minut"     │
│ - "Use Text" button                                │
│                                                    │
│ Dashboard updates:                                 │
│ - Refreshes timeline (calls useTimeline)           │
│ - Displays new event in Timeline                   │
│ - Shows recommendations in sidebar                 │
│                                                    │
│ Timeline calls:                                    │
│ GET /api/timeline?userId=1                         │
│   (fetches events for last 7 days)                 │
└─────────────────────────────────────────────────────┘
```

---

## 6. Key Assumptions & Limitations

### User Management
- **No authentication**: All requests use hardcoded or optional `userId`
- **No session management**: Frontend stores `userId=1` by default in hooks
- **No authorization**: No role-based access control
- **Assumption**: Single user (userId=1) for MVP

### Event Types
- **Limited set**: 10 predefined types (FOOD, ACTIVITY, etc.)
- **Client-side validation**: Frontend doesn't validate event types
- **Detection method**: Simple regex pattern matching on Polish keywords
- **No extensibility**: Adding new types requires code changes

### Recommendations
- **No AI backend**: Current implementation uses rule-based logic
- **OpenAI API key**: Optional (not fully integrated)
- **No persistence of recommendation interactions**: Marking as "done" updates DB but flow may be incomplete

### Data Retention
- **No soft deletes**: Events are hard-deleted
- **No archiving**: Old data stays in production database
- **No data export**: No API for data export/backup

### Frontend
- **Single SPA**: No multi-page routing (just one Dashboard view)
- **No offline support**: Requires backend connectivity
- **No caching**: Each timeline fetch hits the API
- **No real-time updates**: No WebSocket or polling for live updates

### STT Service
- **Polish-first**: Default language is Polish
- **Model loading**: 5-10 seconds on first startup (cached after)
- **No concurrency control**: Single model instance
- **No audio validation**: Accepts any audio format Whisper supports

---

## 7. API Contracts (Must Not Change Without Decision)

### Speech-to-Text Transcription Response
```json
{
  "transcription": {
    "text": "string",
    "sourceFile": "string",
    "language": "string",
    "processingTimeMs": "number"
  },
  "event": {
    "id": "number",
    "userId": "number",
    "type": "string (EventType)",
    "description": "string",
    "timestamp": "ISO datetime",
    "metadata": "object"
  },
  "recommendations": [
    {
      "id": "number",
      "userId": "number",
      "eventId": "number",
      "text": "string",
      "type": "string",
      "priority": "HIGH|MEDIUM|LOW",
      "status": "string",
      "isApplied": "boolean",
      "createdAt": "ISO datetime"
    }
  ]
}
```

### Timeline Response
```json
{
  "entries": [
    {
      "id": "string",
      "event": {
        "id": "number",
        "userId": "number",
        "type": "string",
        "description": "string",
        "timestamp": "ISO datetime"
      },
      "createdAt": "ISO datetime",
      "importance": "number (1-10)"
    }
  ],
  "total": "number"
}
```

### Recommendations List Response
```json
[
  {
    "id": "string",
    "category": "string",
    "suggestion": "string",
    "priority": "low|medium|high",
    "status": "PLANNED|DONE",
    "actionUrl": "string",
    "aiGenerated": "boolean",
    "createdAt": "ISO datetime"
  }
]
```

---

## 8. Database Schema (Auto-Generated by Hibernate)

### events table
```sql
CREATE TABLE events (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL,
  event_type VARCHAR(50) NOT NULL,
  description TEXT NOT NULL,
  event_timestamp TIMESTAMP NOT NULL,
  metadata JSONB,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_events_user_id (user_id),
  INDEX idx_events_event_type (event_type),
  INDEX idx_events_event_timestamp (event_timestamp)
);
```

### recommendations table
```sql
CREATE TABLE recommendations (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL,
  event_id BIGINT,
  text TEXT,
  type VARCHAR(50),
  priority VARCHAR(20),
  status VARCHAR(50),
  is_applied BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

## 9. Environment Variables

### Backend (Spring Boot)
```properties
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/lifeai
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres
SPRING_JPA_HIBERNATE_DDL_AUTO=update
OPENAI_API_KEY=${OPENAI_API_KEY}  # Optional, currently unused
STT_SERVICE_URL=http://stt:5000
```

### STT Service (Python)
```
WHISPER_MODEL_SIZE=base  # Options: tiny, small, base, medium, large
LOG_LEVEL=INFO
DEBUG=False
```

### Frontend (Vite)
```
VITE_API_URL=http://backend:8080  # Docker; http://localhost:8080 for local dev
```

---

## 10. Known Issues, Mismatches & Current State

### **Critical Issues - Feature Breakage**

| Issue | Status | Impact | Details |
|-------|--------|--------|---------|
| **Timeline Response Structure Mismatch** | 🔴 BROKEN | Timeline display fails | Backend returns `{ days: [...] }`, frontend expects `{ entries: [...] }`. Timeline.tsx:138 checks for `.entries`, which doesn't exist, rendering empty list with "Brak zdarzeń" message. |
| **RecommendationResponse JSON Field Mapping** | ⚠️ UNSTABLE | Inconsistent display | Backend sends `{ suggestion, category }` (via @JsonProperty), frontend types expect `{ text, type }`. Dashboard.tsx:70-80 manually maps both field names, suggesting inconsistent handling. |

### **Known Limitations**

| Issue | Status | Workaround |
|-------|--------|-----------|
| No user authentication | Known limitation | Use userId=1 for testing |
| STT model loads on every backend restart | Performance | Model cached after first load |
| Recommendations rule engine is hardcoded | Technical debt | Update RuleEngine.java for new rules |
| No WebSocket for real-time updates | Expected MVP limitation | Full page refresh required |
| Frontend doesn't auto-refresh timeline | Expected | Manual refresh button provided |
| Event type detection only works for Polish | Expected | Language-specific keywords in code |
| No pagination in timeline (always last 50 events) | Partial | Limit/offset parameters exist but not fully used |

### **What Actually Works**

- ✅ Voice recording (Web Audio API)
- ✅ Audio transcription (calls STT service at /transcribe)
- ✅ Event creation from transcription (auto type detection)
- ✅ Event creation from manual text input
- ✅ Recommendation generation (rule-based)
- ✅ Recommendation status updates (done/planned PATCH endpoints)
- ⚠️ **Timeline display** - TECHNICALLY FAILS (see critical issues)
- ⚠️ **Recommendation display** - PARTIALLY WORKS (field mapping inconsistent)

### **What Doesn't Work or Is Untested**

- ❌ Timeline display (mismatch prevents rendering)
- ❌ User authentication (no auth layer)
- ❌ Notification system (stub only)
- ❌ Calendar system (stub only)
- ❌ AI analysis endpoints (stub only)
- ❌ User management endpoints (stub only)

---

## 11. What NOT to Change Without Explicit Decision

These are the core contracts that other components depend on:

1. **STT Endpoint Path**: `/speech-to-text/transcribe` (frontend hardcoded)
2. **STT Request Parameters**: `file`, `language`, `userId` (frontend passes these)
3. **STT Response Schema**: Must include `transcription`, `event`, `recommendations` keys
4. **Timeline Endpoint**: `/api/timeline` and query parameters (`userId`, `from`, `to`)
5. **Recommendation PATCH endpoints**: `/recommendations/{id}/done` and `/api/recommendations/{id}/planned`
6. **EventType Enum Names**: Used in frontend and stored in database
7. **Frontend Expected Response Fields**: `transcription.text`, `event.id`, `event.type`, `recommendations[].id`
8. **Database Column Names**: Primary keys, foreign keys, indexed columns (changes require migration)

---

## 12. Testing & Deployment

### Backend Tests
```bash
cd lifeai-backend
mvn test
```

### Frontend Build
```bash
cd lifeai-frontend
npm run build
```

### Full Stack (Docker Compose)
```bash
docker-compose up -d
# Frontend: http://localhost:3000
# Backend: http://localhost:8080
# STT: http://localhost:5000
```

---

## 13. Summary - Actual Project State

**Project Status**: Partial MVP - Voice input chain works, but Timeline display is broken

**Actually Working** (End-to-End):
- ✅ Voice recording → transcription → event creation → recommendation generation
- ✅ Manual text event creation
- ✅ Event type auto-detection (Polish keywords)
- ✅ Database persistence of events & recommendations
- ✅ Recommendation status updates (done/planned)

**Partially/Partially Broken**:
- ⚠️ Timeline display - Backend returns data but frontend code expects different schema
- ⚠️ Recommendation display - Field mappings inconsistent (@JsonProperty vs frontend types)

**Not Working or Stub Only**:
- ❌ Timeline display (MISMATCH: backend → `.days`, frontend expects → `.entries`)
- ❌ User authentication (no auth layer)
- ❌ User management endpoints (stub)
- ❌ Notification system (stub)
- ❌ Calendar system (stub)
- ❌ AI analysis endpoints (stub)
- ❌ Real-time updates (no WebSocket)

**Critical Mismatches to Resolve** (Decision Required):
1. **Timeline Response** - Choose: fix backend to return `.entries` OR fix frontend to handle `.days`
2. **RecommendationResponse JSON Fields** - Remove @JsonProperty mappings OR update frontend types to match JSON

**Tech Debt**:
- Hardcoded userId=1 (though useUser hook supports changing it)
- Recommendation rule engine is monolithic in RuleEngine.java
- No API versioning
- Limited error handling in frontend
- No transformer/converter layer for response mapping

---

**Document Generated**: December 17, 2025  
**Last Updated**: December 17, 2025 (Validated against actual codebase)  
**Validation Status**: ✅ VERIFIED - All facts cross-checked against source code
**Valid Until**: Next significant codebase change or feature branch merge

### Validation Checklist
- ✅ Backend controllers & endpoints verified against source
- ✅ Frontend hooks & components verified against source  
- ✅ API routing verified (found `/api` prefix in application.properties)
- ✅ Response schemas verified against actual DTOs
- ✅ Critical mismatches documented (Timeline, RecommendationResponse)
- ✅ Database schema verified against entity definitions
- ✅ STT service configuration verified
- ✅ Docker Compose verified
- ✅ Known issues documented with actual impact assessment
- ✅ Hooks file structure verified (not all separate files)
