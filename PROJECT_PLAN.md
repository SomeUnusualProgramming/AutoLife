# LifeAI - Projekt Plan i Instrukcje

## 📋 Spis Treści
1. [Przegląd Systemu](#przegląd-systemu)
2. [Architektura Mikroserwisów](#architektura-mikroserwisów)
3. [Komponenty](#komponenty)
4. [Data Flow](#data-flow)
5. [Instalacja i Setup](#instalacja-i-setup)
6. [API Endpoints](#api-endpoints)
7. [Konfiguracja](#konfiguracja)
8. [Uruchamianie](#uruchamianie)
9. [Debugowanie](#debugowanie)
10. [Wdrażanie](#wdrażanie)

---

## Przegląd Systemu

**LifeAI** to aplikacja do zarządzania zdarzeniami życiowymi (diet, aktywność, sny, nastrój itp.) z następującymi cechami:

- ✅ **100% Offline** - Całkowita prywatność, brak połączenia z API
- ✅ **Mowa → Tekst** - Lokalny Whisper (Speech-to-Text)
- ✅ **Inteligentne Rekomendacje** - Rule engine do generowania sugestii
- ✅ **Timeline** - Wizualizacja zdarzeń w czasie
- ✅ **Mikroserwisy** - Modułowa, skalowalna architektura

### Technologia Stack

| Komponent | Technologia | Port |
|-----------|------------|------|
| Frontend | React 18 + Vite + Tailwind | 3000 |
| Backend | Spring Boot 3.2 + Java 21 | 8080 |
| STT Service | Python + FastAPI + Faster-Whisper | 5000 |
| Database | PostgreSQL 16 | 5432 |

---

## Architektura Mikroserwisów

```
┌─────────────────────────────────────────────────────────────┐
│                   FRONTEND (React)                          │
│  • Audio Recording (Web Audio API)                          │
│  • Event Timeline Visualization                            │
│  • Recommendations Display                                 │
│  • User Interface                                          │
└────────────┬──────────────────────────────────────────────┘
             │ HTTP/REST (port 8080)
             ↓
┌─────────────────────────────────────────────────────────────┐
│            BACKEND API (Spring Boot 3.2)                    │
│                                                              │
│  Controllers:                                               │
│  • SpeechToTextController (/speech-to-text/*)              │
│  • EventController (/events/*)                             │
│  • RecommendationController (/recommendations/*)           │
│  • TimelineController (/timeline/*)                        │
│  • UserController (/users/*)                               │
│                                                              │
│  Services:                                                  │
│  • WhisperSpeechToTextService (→ STT Microservice)         │
│  • EventService                                            │
│  • RecommendationService + RuleEngine                      │
│  • TimelineService                                         │
│  • CalendarService                                         │
│                                                              │
│  Database: PostgreSQL 16                                    │
└────────────┬──────────────────────────────────────────────┘
             │ HTTP/REST (port 5000)
             ↓
┌─────────────────────────────────────────────────────────────┐
│     STT MICROSERVICE (Python + FastAPI)                     │
│                                                              │
│  Endpoints:                                                 │
│  • POST /transcribe - Audio to Text                        │
│  • GET /health - Health Check                              │
│                                                              │
│  Service:                                                   │
│  • WhisperService (Faster-Whisper Model)                   │
│  • Audio Processing (WAV, MP3, M4A, OGG, WEBM)            │
│  • Language Detection                                       │
│                                                              │
│  100% Offline, CPU/GPU Optimized, ~140MB Model             │
└─────────────────────────────────────────────────────────────┘
```

---

## Komponenty

### 1. Frontend (`lifeai-frontend/`)

**Plik:** `src/`

**Komponenty:**
- **AudioRecorder** - Nagrywanie audio za pomocą Web Audio API
- **Dashboard** - Główny widok aplikacji
- **Timeline** - Wizualizacja zdarzeń
- **Recommendations** - Wyświetlanie rekomendacji
- **Header** - Nagłówek aplikacji

**Hooks:**
- `useAudioRecorder()` - Obsługa nagrywania
- `useSpeechToText()` - Transkrypcja audio
- `useTimeline()` - Pobieranie timeline
- `useRecommendations()` - Pobieranie rekomendacji

**Serwisy:**
- `api.ts` - Axios client z metodami dla każdego endpointa

**Typy:**
- `Event`, `Recommendation`, `Timeline`, `TranscriptionResult`

---

### 2. Backend (`lifeai-backend/`)

#### 2.1 Kontrolery

**SpeechToTextController** (`/speech-to-text/`)
```
POST /transcribe
  - Parametry: file (MultipartFile), language (optional), userId (optional)
  - Wysyła audio do STT microservice
  - Jeśli userId → tworzy Event i Recommendations

POST /transcribe-bytes
  - Parametry: audioBytes, mimeType, language, userId
  - Alternatywny endpoint dla bajtów audio
```

**EventController** (`/events/`)
```
POST /events - Tworzenie eventu
GET /events - Lista eventów
GET /events/{id} - Szczegóły eventu
PUT /events/{id} - Aktualizacja
DELETE /events/{id} - Usunięcie
GET /events/date-range - Filtrowanie po dacie
```

**RecommendationController** (`/recommendations/`)
```
GET /recommendations - Lista rekomendacji
GET /recommendations/{id} - Szczegóły
PATCH /recommendations/{id}/done - Oznaczenie jako wykonane
PATCH /recommendations/{id}/planned - Oznaczenie jako zaplanowane
GET /recommendations/user/{userId} - Rekomendacje dla użytkownika
```

**TimelineController** (`/timeline/`)
```
GET /timeline - Timeline zdarzeń (grouped by date)
GET /timeline?limit=30&offset=0 - Paginacja
```

**UserController** (`/users/`)
```
POST /users - Rejestracja
GET /users/{id} - Profil użytkownika
PUT /users/{id} - Aktualizacja profilu
```

#### 2.2 Serwisy

**WhisperSpeechToTextService**
- Implementuje `SpeechToTextService`
- Komunikuje się z Python STT microservice
- Wysyła multipart/form-data request do `http://localhost:5000/transcribe`
- Parsuje JSON response
- Timeout: 5 minut (dla długich nagrań)

```java
public String transcribeAudio(MultipartFile audioFile, String language)
public String transcribeAudio(byte[] audioBytes, String mimeType, String language)
```

**EventService**
- CRUD operacje na eventach
- Detekcja typu eventu z tekstu (food, activity, sleep itp.)
- Tworzenie wpisów w kalendarz dla ważnych eventów
- Filtrowanie po userId, type, date range

**RecommendationService**
- Generowanie rekomendacji za pomocą RuleEngine
- Filtrowanie po typu, priorytecie
- Oznaczanie jako "wykonane" lub "zaplanowane"

**RecommendationRuleEngine**
- Analizuje obiekty Event
- Aplikuje reguły biznesowe:
  - FOOD → zasugeruj ACTIVITY
  - SYMPTOM → zasugeruj DOCTOR_VISIT
  - LOW_MOOD → zasugeruj ACTIVITY
- Extensible rule system

#### 2.3 Encje (Entities)

```sql
users (id, email, password, firstName, lastName, createdAt, updatedAt)

events (id, userId, type, description, timestamp, metadata JSON, createdAt, updatedAt)
  - Types: FOOD, ACTIVITY, SLEEP, MOOD, MEDICATION, SYMPTOM, DOCTOR_VISIT, WEIGHT, OTHER

recommendations (id, userId, eventId, type, text, priority, isApplied, appliedAt, createdAt)
  - Types: PHYSICAL_ACTIVITY, NUTRITION, MEDICAL_CHECKUP, REST, MEDICATION_REMINDER
  - Priorities: HIGH, MEDIUM, LOW

timelines (id, userId, eventId, date, metadata JSON, createdAt)

calendars (id, userId, title, description, eventId, startDate, endDate, type)

notifications (id, userId, type, title, message, isRead, createdAt)
```

---

### 3. STT Microservice (`lifeai-stt/`)

**Struktura:**
```
lifeai-stt/
  ├── app/
  │   ├── __init__.py
  │   ├── main.py          # FastAPI app + routes
  │   ├── services.py      # WhisperService
  │   ├── models.py        # Pydantic models
  │   └── config.py        # Settings
  ├── requirements.txt     # Python dependencies
  ├── Dockerfile
  ├── .env.example
  └── README.md
```

#### 3.1 API Contract

**POST /transcribe**

Request:
```http
POST /transcribe
Content-Type: multipart/form-data

file: <audio_file> (WAV, MP3, M4A, OGG, WEBM)
language: pl (optional, default="pl")
```

Response (200 OK):
```json
{
  "text": "Dzisiaj jadłem obiad i poszedłem na spacer",
  "language": "pl",
  "confidence": 0.95,
  "processing_time_ms": 2340,
  "model": "faster-whisper"
}
```

**GET /health**

Response (200 OK):
```json
{
  "status": "healthy",
  "model": "faster-whisper",
  "version": "0.10.0"
}
```

#### 3.2 WhisperService

```python
class WhisperService:
    def __init__(self, model_size: str = "base"):
        # Ładuje model Faster-Whisper
        # device="cpu", compute_type="int8"
    
    def transcribe(audio_bytes: bytes, language: str = "pl") -> Tuple[str, str, float, int]:
        # Transkrybuje audio i zwraca (text, detected_language, confidence, processing_time_ms)
    
    def is_healthy() -> bool:
        # Sprawdza czy model jest załadowany
```

#### 3.3 Obsługiwane Formaty Audio
- WAV
- MP3
- M4A (AAC)
- OGG
- WEBM

#### 3.4 Rozmiary Modelu

| Size | Rozmiar | Prędkość | Dokładność | Rekomendacja |
|------|---------|----------|-----------|--------------|
| tiny | 40MB | Najszybszy | ⭐⭐ | Testowanie |
| base | 140MB | Szybki | ⭐⭐⭐ | ✅ Default |
| small | 465MB | Normalny | ⭐⭐⭐⭐ | Produkcja |
| medium | 1.5GB | Wolny | ⭐⭐⭐⭐⭐ | High accuracy |
| large | 3GB | Najwolniejszy | ⭐⭐⭐⭐⭐ | Best accuracy |

---

## Data Flow

### Kompletny Flow: Audio → Event → Recommendation

```
1. USER SIDE - Frontend
   └─→ Użytkownik mówi: "Dzisiaj jadłem porcję makaronu"
       Format: WAV/MP3/M4A
       Size: ~200KB

2. FRONTEND → BACKEND
   └─→ POST /api/speech-to-text/transcribe
       Content-Type: multipart/form-data
       Body:
         - file: audio.webm
         - language: pl
         - userId: 123
       Timeout: 10 sekund

3. BACKEND RECEIVES REQUEST
   └─→ SpeechToTextController.transcribeAudio()
       Validates audio file
       Extracts userId
       Logs: "Transcribing audio file: audio.webm"

4. BACKEND → STT MICROSERVICE
   └─→ WhisperSpeechToTextService.transcribeAudio()
       Creates multipart request
       POST http://localhost:5000/transcribe
       Body: file + language="pl"
       Timeout: 5 minutes

5. STT MICROSERVICE PROCESSES
   └─→ FastAPI /transcribe endpoint
       Validates audio format
       WhisperService.transcribe()
       → Loads Faster-Whisper model
       → Reads audio_bytes
       → Runs inference
       → Returns (text, language, confidence, processing_time_ms)
       Logs file size and processing time

6. STT RETURNS RESPONSE
   └─→ HTTP 200
       {
         "text": "Dzisiaj jadłem porcję makaronu",
         "language": "pl",
         "confidence": 0.95,
         "processing_time_ms": 2340,
         "model": "faster-whisper"
       }

7. BACKEND PARSES RESPONSE
   └─→ WhisperSpeechToTextService.transcribeAudioBytes()
       JsonNode jsonResponse = objectMapper.readTree(response)
       String transcription = jsonResponse.get("text").asText()
       Returns: "Dzisiaj jadłem porcję makaronu"

8. BACKEND CREATES TRANSCRIPTION RESPONSE
   └─→ TranscriptionResponse:
       - text: "Dzisiaj jadłem porcję makaronu"
       - language: "pl"
       - processingTimeMs: 2340

9. BACKEND CREATES EVENT
   └─→ SpeechToTextController.createEventFromTranscription()
       EventType detectedType = detectEventType(text)
       → Analiza: contains "jad", "obiad" → FOOD
       
       CreateEventRequest:
       - userId: 123
       - type: FOOD
       - description: "Dzisiaj jadłem porcję makaronu"
       - timestamp: 2025-12-16T13:05:00
       - metadata: {source: "speech-to-text", textLength: 35}
       
       EventService.createEvent()
       → Saves to database
       → Returns EventResponse with id=456

10. BACKEND GENERATES RECOMMENDATIONS
    └─→ RecommendationService.generateRecommendationsForEvent()
        RuleEngine.analyzeEvent(event)
        
        Rule: "FOOD → suggest PHYSICAL_ACTIVITY"
        Creates Recommendation:
        - userId: 123
        - eventId: 456
        - type: PHYSICAL_ACTIVITY
        - text: "Rozważ spacer po posiłku, aby ułatwić trawienie"
        - priority: MEDIUM
        
        Saves to database

11. BACKEND RETURNS COMPLETE RESPONSE
    └─→ HTTP 200
        {
          "transcription": {
            "text": "Dzisiaj jadłem porcję makaronu",
            "language": "pl",
            "processingTimeMs": 2340
          },
          "event": {
            "id": 456,
            "userId": 123,
            "type": "FOOD",
            "description": "Dzisiaj jadłem porcję makaronu",
            "timestamp": "2025-12-16T13:05:00"
          },
          "recommendations": [
            {
              "id": 789,
              "type": "PHYSICAL_ACTIVITY",
              "text": "Rozważ spacer po posiłku...",
              "priority": "MEDIUM"
            }
          ]
        }

12. FRONTEND RECEIVES RESPONSE
    └─→ Updates UI:
        ✓ Shows transcription: "Dzisiaj jadłem porcję makaronu"
        ✓ Displays event type: 🍽️ FOOD
        ✓ Shows recommendation: "Rozważ spacer..."
        ✓ Adds to timeline
        ✓ Updates statistics

13. COMPLETE ✓
    └─→ Event saved to database
        Recommendation generated
        Timeline updated
        User sees results in real-time
```

---

## Instalacja i Setup

### Wymagania
- Git
- Docker + Docker Compose (opcjonalne, ale rekomendowane)
- LUB:
  - Java 21+ (do backend)
  - Node.js 18+ (do frontend)
  - Python 3.10+ (do STT)
  - PostgreSQL 16

### Quick Start (Docker Compose)

```bash
# 1. Klonuj projekt
git clone <repo-url>
cd AutoLife

# 2. Utwórz .env plik
cat > .env << EOF
OPENAI_API_KEY=sk-... (jeśli potrzebny)
WHISPER_MODEL_SIZE=base
LOG_LEVEL=INFO
DEBUG=False
EOF

# 3. Uruchom docker-compose
docker-compose up -d

# 4. Czekaj na inicjalizację (~2-3 minuty)
docker-compose logs -f stt  # Czekaj na "Model loaded successfully"

# 5. Sprawdź dostępność
curl http://localhost:3000   # Frontend
curl http://localhost:8080   # Backend
curl http://localhost:5000/health  # STT Service

# 6. Otwórz w przeglądarce
http://localhost:3000
```

### Instalacja Lokalna (Development)

#### Terminal 1: PostgreSQL
```bash
docker run --name postgres-lifeai \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=lifeai \
  -p 5432:5432 \
  postgres:16-alpine
```

#### Terminal 2: STT Microservice
```bash
cd lifeai-stt

# Utwórz virtual environment
python -m venv venv

# Windows
venv\Scripts\activate
# macOS/Linux
source venv/bin/activate

# Zainstaluj zależności
pip install -r requirements.txt

# Uruchom serwis
python -m uvicorn app.main:app --host 0.0.0.0 --port 5000 --reload
```

#### Terminal 3: Backend
```bash
cd lifeai-backend

# Build
mvn clean package -DskipTests

# Uruchom
java -Dspring.profiles.active=local -jar target/lifeai-backend-1.0.0-SNAPSHOT.jar

# Lub w IDE: klik "Run" w LifeAiBackendApplication.java
```

#### Terminal 4: Frontend
```bash
cd lifeai-frontend

# Zainstaluj zależności
npm install

# Uruchom dev server
npm run dev

# Otwórz: http://localhost:5173
```

---

## API Endpoints

### Frontend → Backend

#### Speech-to-Text
```
POST /api/speech-to-text/transcribe
  Query: file (MultipartFile), language (optional), userId (optional)
  Response: { transcription, event, recommendations }

POST /api/speech-to-text/transcribe-bytes
  Query: audioBytes (Blob), mimeType, language, userId
  Response: { transcription, event, recommendations }
```

#### Events
```
GET /api/events
  Query: userId, type, startDate, endDate, limit, offset
  Response: List<EventResponse>

GET /api/events/{id}
  Response: EventResponse

POST /api/events
  Body: CreateEventRequest
  Response: EventResponse

PUT /api/events/{id}
  Body: CreateEventRequest
  Response: EventResponse

DELETE /api/events/{id}
  Response: 204 No Content
```

#### Recommendations
```
GET /api/recommendations
  Query: userId, type, priority, limit, offset
  Response: List<RecommendationResponse>

GET /api/recommendations/{id}
  Response: RecommendationResponse

PATCH /api/recommendations/{id}/done
  Response: RecommendationResponse

PATCH /api/recommendations/{id}/planned
  Response: RecommendationResponse
```

#### Timeline
```
GET /api/timeline
  Query: userId, limit, offset
  Response: TimelineResponse (grouped by date)
```

#### Users
```
POST /api/users
  Body: CreateUserRequest
  Response: UserResponse

GET /api/users/{id}
  Response: UserResponse

PUT /api/users/{id}
  Body: CreateUserRequest
  Response: UserResponse
```

### Backend → STT Service

```
POST /transcribe
  Form-data: file (binary), language (string, optional)
  Response: { text, language, confidence, processing_time_ms, model }

GET /health
  Response: { status, model, version }
```

---

## Konfiguracja

### Backend (application.yml)

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
    show-sql: false
  
  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 50MB

server:
  port: 8080

stt:
  service-url: ${STT_SERVICE_URL:http://localhost:5000}
  default-language: pl
  connect-timeout: 30000
  read-timeout: 300000

logging:
  level:
    com.lifeai: DEBUG
    org.springframework: INFO
```

### STT Service (.env)

```
APP_NAME=LifeAI Speech-to-Text Service
APP_VERSION=1.0.0
DEBUG=False
WHISPER_MODEL_SIZE=base
LOG_LEVEL=INFO
```

### Frontend (environment variables)

```
VITE_API_URL=http://localhost:8080
```

---

## Uruchamianie

### Development Mode

```bash
# Docker Compose
docker-compose up

# Check status
docker-compose ps

# View logs
docker-compose logs -f backend
docker-compose logs -f stt
docker-compose logs -f frontend

# Stop
docker-compose down

# Remove volumes (wipe database)
docker-compose down -v
```

### Production Mode

```bash
# Build images
docker-compose build

# Run with specific env
docker-compose -f docker-compose.yml up -d

# Use scale for multiple backend instances
docker-compose up -d --scale backend=3

# Health check
curl http://localhost:8080/health
curl http://localhost:5000/health
curl http://localhost:3000
```

---

## Debugowanie

### Backend Logs
```bash
docker-compose logs -f backend --tail=100

# Search for errors
docker-compose logs backend | grep ERROR

# View specific service
docker-compose logs stt
```

### Python STT Logs
```bash
# Local development
tail -f /tmp/lifeai-stt.log

# Docker
docker-compose exec stt tail -f /var/log/stt.log
```

### Database Queries
```bash
# Connect to PostgreSQL
docker-compose exec postgres psql -U postgres -d lifeai

# Common queries
SELECT * FROM events;
SELECT * FROM recommendations;
SELECT * FROM users;
```

### Frontend Console
```
Browser DevTools → Console tab
Check for:
- API call errors (network tab)
- Component errors (console)
- Network requests to backend
```

### Health Checks

```bash
# Backend health
curl http://localhost:8080/health

# STT health
curl http://localhost:5000/health

# Frontend (should return HTML)
curl http://localhost:3000
```

---

## Wdrażanie

### Cloud Deployment (AWS/GCP/Azure)

#### 1. Przygotuj środowisko

```bash
# Push do container registry
docker build -t lifeai-backend lifeai-backend/
docker tag lifeai-backend:latest myregistry/lifeai-backend:latest
docker push myregistry/lifeai-backend:latest

# Repeat dla stt i frontend
```

#### 2. Kubernetes Deployment (opcjonalne)

```yaml
apiVersion: v1
kind: Deployment
metadata:
  name: lifeai-backend
spec:
  replicas: 3
  template:
    spec:
      containers:
      - name: backend
        image: myregistry/lifeai-backend:latest
        ports:
        - containerPort: 8080
        env:
        - name: STT_SERVICE_URL
          value: http://lifeai-stt:5000
        - name: SPRING_DATASOURCE_URL
          valueFrom:
            secretKeyRef:
              name: db-credentials
              key: url
```

#### 3. Environment Configuration

```bash
# Development
export STT_SERVICE_URL=http://localhost:5000
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/lifeai

# Production
export STT_SERVICE_URL=http://stt-service:5000
export SPRING_DATASOURCE_URL=jdbc:postgresql://postgres-prod:5432/lifeai
export OPENAI_API_KEY=sk-prod-...
```

---

## Troubleshooting

### STT Service nie startuje

```bash
# Sprawdź czy port 5000 jest wolny
lsof -i :5000

# Czy Python 3.10+ zainstalowany?
python --version

# Czy requirements zainstalowane?
pip list | grep faster-whisper

# Spróbuj załadować model ręcznie
python -c "from faster_whisper import WhisperModel; WhisperModel('base')"
```

### Backend nie łączy się z STT

```bash
# Sprawdź czy STT serwis działa
curl http://localhost:5000/health

# Sprawdź logs backendu
grep "STT" logs/backend.log

# Ustaw poprawny URL
export STT_SERVICE_URL=http://stt:5000  # Docker
export STT_SERVICE_URL=http://localhost:5000  # Local
```

### PostgreSQL Connection Error

```bash
# Sprawdź czy PostgreSQL działa
docker-compose ps postgres

# Sprawdź czy baza lifeai istnieje
docker-compose exec postgres psql -U postgres -l

# Reset bazy
docker-compose down -v
docker-compose up postgres
```

### Frontend Connection Refused

```bash
# Sprawdź VITE_API_URL
echo $VITE_API_URL

# Backend musi być dostępny
curl http://backend:8080/health

# Frontend dev server
npm run dev
```

---

## Architektura Folderu

```
AutoLife/
├── lifeai-frontend/          # React SPA
│   ├── src/
│   │   ├── components/       # React components
│   │   ├── hooks/            # Custom hooks
│   │   ├── services/         # API client
│   │   ├── types/            # TypeScript types
│   │   └── App.tsx
│   ├── Dockerfile
│   └── package.json
│
├── lifeai-backend/           # Spring Boot API
│   ├── src/main/java/com/lifeai/
│   │   ├── controller/       # REST endpoints
│   │   ├── service/          # Business logic
│   │   ├── entity/           # JPA entities
│   │   ├── repository/       # Database access
│   │   ├── dto/              # Data transfer objects
│   │   ├── recommendation/   # Rule engine
│   │   └── config/           # Configuration
│   ├── src/main/resources/
│   │   └── application.yml
│   ├── Dockerfile
│   └── pom.xml
│
├── lifeai-stt/               # Python STT Service
│   ├── app/
│   │   ├── main.py          # FastAPI app
│   │   ├── services.py      # WhisperService
│   │   ├── models.py        # Pydantic models
│   │   └── config.py        # Settings
│   ├── requirements.txt
│   ├── Dockerfile
│   ├── .env.example
│   └── README.md
│
├── docker-compose.yml        # Orchestration
├── ARCHITECTURE.md           # System architecture
├── PROJECT_PLAN.md          # This file
└── README.md
```

---

## API Response Examples

### Successful Transcription + Event Creation

```bash
curl -X POST http://localhost:8080/api/speech-to-text/transcribe \
  -F "file=@audio.wav" \
  -F "language=pl" \
  -F "userId=123"
```

Response:
```json
{
  "transcription": {
    "text": "Dzisiaj jadłem obiad i poszedłem na spacer",
    "sourceFile": "audio.wav",
    "language": "pl",
    "processingTimeMs": 2340
  },
  "event": {
    "id": 456,
    "userId": 123,
    "type": "FOOD",
    "description": "Dzisiaj jadłem obiad i poszedłem na spacer",
    "timestamp": "2025-12-16T13:05:00"
  }
}
```

### Get Timeline

```bash
curl http://localhost:8080/api/timeline?userId=123
```

Response:
```json
{
  "timeline": [
    {
      "date": "2025-12-16",
      "events": [
        {
          "id": 456,
          "type": "FOOD",
          "description": "Dzisiaj jadłem obiad...",
          "timestamp": "2025-12-16T13:05:00"
        }
      ],
      "stats": {
        "total": 1,
        "byType": {"FOOD": 1}
      }
    }
  ]
}
```

### Get Recommendations

```bash
curl http://localhost:8080/api/recommendations?userId=123
```

Response:
```json
{
  "recommendations": [
    {
      "id": 789,
      "userId": 123,
      "eventId": 456,
      "type": "PHYSICAL_ACTIVITY",
      "text": "Rozważ spacer po posiłku, aby ułatwić trawienie",
      "priority": "MEDIUM",
      "isApplied": false,
      "createdAt": "2025-12-16T13:05:00"
    }
  ]
}
```

---

## Konwencje Kodu

### Backend (Java)

- Package naming: `com.lifeai.*`
- Annotations: `@Service`, `@Controller`, `@Repository`, `@Entity`
- Logging: `private static final Logger log = LoggerFactory.getLogger(ClassName.class);`
- Transactions: `@Transactional` na service layer
- RestTemplate timeout: Connection=30s, Read=300s

### Frontend (React)

- Components: PascalCase (e.g., `AudioRecorder.tsx`)
- Hooks: camelCase (e.g., `useAudioRecorder()`)
- Styling: Tailwind classes
- API calls: `speechToTextApi.transcribeAudio()`

### Python (STT)

- Models: Pydantic `BaseModel`
- Logging: `logging.getLogger(__name__)`
- Async: FastAPI async routes
- Error handling: HTTPException with status codes

---

## Performance Targets

| Metrika | Target | Status |
|---------|--------|--------|
| Frontend Load | <2s | ✅ |
| Audio Upload | <3s | ✅ |
| STT Processing | 1-3s (depends on audio length) | ✅ |
| Event Creation | <500ms | ✅ |
| Recommendation Gen | <1s | ✅ |
| Timeline Load | <1s | ✅ |
| Model Loading (first) | <10s | ✅ |
| Model Loading (cached) | instant | ✅ |

---

## Future Enhancements

- [ ] Real-time streaming transcription
- [ ] Multi-language support with translation
- [ ] GPU acceleration (CUDA/cuDNN)
- [ ] Async job queue (Celery/Bull)
- [ ] WebSocket for live updates
- [ ] Mobile app (React Native)
- [ ] Advanced analytics dashboard
- [ ] ML-based recommendation improvement
- [ ] Voice biometrics/speaker recognition
- [ ] Batch processing API

---

## Support & Contacts

- **Repository**: [AutoLife GitHub]
- **Issues**: Report via GitHub Issues
- **Documentation**: See ARCHITECTURE.md

---

**Last Updated**: December 2025
**Version**: 1.0.0
**Status**: Production Ready ✅
