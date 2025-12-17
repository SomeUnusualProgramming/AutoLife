# AutoLife Repository Documentation

## 📌 Project Overview

**AutoLife** is a comprehensive health and lifestyle management application with a **microservices architecture**. It combines a modern React frontend, a feature-rich Spring Boot backend, and an offline speech-to-text microservice to help users track health events, receive personalized recommendations, and manage their wellness journey.

**Project Structure:**
- **Frontend**: React 18 with TypeScript, Vite, Tailwind CSS
- **Backend**: Spring Boot 3.2 (Java 21) with PostgreSQL
- **STT Service**: Python FastAPI with Faster-Whisper (Offline speech recognition)

---

## 🏗️ System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    Frontend (React)                              │
│            Vite | TypeScript | Tailwind CSS                     │
│  Audio recording, Event visualization, Recommendations display  │
└────────────────┬────────────────────────────────────────────────┘
                 │ HTTP/REST (port 3000)
                 ↓
┌─────────────────────────────────────────────────────────────────┐
│          Backend API (Spring Boot 3.2 Java 21)                  │
│              PostgreSQL 15 | Flyway Migrations                  │
│                                                                  │
│  Controllers:                                                    │
│  - SpeechToTextController (/api/speech-to-text/*)              │
│  - EventController (/api/events/*)                             │
│  - RecommendationController (/api/recommendations/*)           │
│  - TimelineController (/api/timeline/*)                        │
│  - UserController (/api/users/*)                               │
│  - AiController (/api/ai/*)                                    │
│  - CalendarController (/api/calendars/*)                       │
│  - NotificationController (/api/notifications/*)               │
│                                                                  │
│  Services:                                                       │
│  - WhisperSpeechToTextService → STT Microservice               │
│  - EventService                                                 │
│  - RecommendationService + RuleEngine                          │
│  - TimelineService                                              │
│  - UserService                                                  │
│  - CalendarService                                              │
│  - NotificationService                                          │
│  - LifeAiService (AI integration)                              │
└────────────────┬────────────────────────────────────────────────┘
                 │ HTTP/REST (port 5000)
                 ↓
┌─────────────────────────────────────────────────────────────────┐
│   STT Microservice (Python FastAPI + Faster-Whisper)            │
│                                                                  │
│  - POST /transcribe - Audio to Text                            │
│  - GET /health - Service health check                          │
│                                                                  │
│  Features:                                                       │
│  - 100% Offline (no cloud API calls)                           │
│  - Multi-language support (Polish, English, etc.)              │
│  - CPU/GPU optimized (INT8 quantization)                       │
│  - Supported formats: WAV, MP3, M4A, OGG, WEBM                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 📁 Project Structure

```
AutoLife/
├── lifeai-frontend/
│   ├── src/
│   │   ├── components/          # Reusable React components
│   │   ├── hooks/               # Custom React hooks
│   │   ├── pages/               # Page components
│   │   ├── services/            # API clients
│   │   ├── types/               # TypeScript definitions
│   │   ├── App.tsx              # Main component
│   │   ├── main.tsx             # Entry point
│   │   └── index.css            # Global styles
│   ├── dist/                    # Production build output
│   ├── package.json             # Dependencies & scripts
│   ├── vite.config.ts           # Vite configuration
│   ├── tsconfig.json            # TypeScript config
│   ├── tailwind.config.js       # Tailwind CSS config
│   └── Dockerfile               # Docker image
│
├── lifeai-backend/
│   ├── src/main/java/com/lifeai/
│   │   ├── config/              # Spring configuration
│   │   ├── controller/          # REST API endpoints
│   │   ├── service/             # Business logic
│   │   ├── repository/          # Data access (JPA)
│   │   ├── entity/              # JPA entities
│   │   ├── dto/                 # Request/response objects
│   │   ├── ai/                  # AI integration
│   │   ├── recommendation/      # Recommendation engine
│   │   ├── exception/           # Custom exceptions
│   │   └── LifeAiBackendApplication.java
│   ├── src/main/resources/
│   │   ├── application.yml      # Main config
│   │   ├── application-dev.yml  # Dev profile
│   │   └── db/migration/        # Flyway migrations
│   ├── pom.xml                  # Maven dependencies
│   ├── Dockerfile               # Docker image
│   └── target/                  # Build output
│
├── lifeai-stt/
│   ├── app/
│   │   ├── main.py              # FastAPI application
│   │   ├── services.py          # WhisperService logic
│   │   ├── models.py            # Pydantic models
│   │   └── config.py            # Configuration
│   ├── requirements.txt         # Python dependencies
│   ├── Dockerfile               # Docker image
│   └── .env.example             # Environment template
│
├── docker-compose.yml           # Multi-container orchestration
├── ARCHITECTURE.md              # Detailed architecture
└── repo.md                       # This file
```

---

## 🚀 Tech Stack

### Frontend
- **React 18** - Modern UI library
- **TypeScript** - Type-safe development
- **Vite** - Ultra-fast build tool
- **Tailwind CSS** - Utility-first CSS framework
- **Axios** - HTTP client
- **ESLint** - Code quality

### Backend
- **Spring Boot 3.2.1** - Application framework
- **Java 21** - Programming language
- **Spring Data JPA** - ORM and data access
- **Spring Web** - REST API support
- **Spring Validation** - Bean validation
- **PostgreSQL 15** - Relational database
- **Flyway 9.22.3** - Database migrations
- **Lombok** - Boilerplate reduction
- **JUnit & TestContainers** - Testing

### STT Microservice
- **FastAPI** - Modern async web framework
- **Faster-Whisper** - Optimized speech-to-text
- **Pydantic** - Data validation
- **Python 3.10+** - Runtime
- **Uvicorn** - ASGI server

---

## 📋 Key Components

### 1. Frontend (React)
**Location:** `lifeai-frontend/`

**Features:**
- Audio recording and file upload
- Real-time event visualization
- Timeline view of health events
- Recommendation display and tracking
- User profile and settings management
- Responsive design with Tailwind CSS

**Development:**
```bash
cd lifeai-frontend
npm install
npm run dev  # http://localhost:3000
```

**Build:**
```bash
npm run build  # Production bundle
npm run lint   # Code quality check
```

---

### 2. Backend (Spring Boot)
**Location:** `lifeai-backend/`

#### Controllers
| Controller | Endpoints | Purpose |
|------------|-----------|---------|
| **SpeechToTextController** | `/api/speech-to-text/*` | Audio transcription |
| **EventController** | `/api/events/*` | Health event CRUD |
| **RecommendationController** | `/api/recommendations/*` | Recommendations management |
| **TimelineController** | `/api/timeline/*` | Timeline view |
| **UserController** | `/api/users/*` | User management |
| **AiController** | `/api/ai/*` | AI integration |
| **CalendarController** | `/api/calendars/*` | Calendar management |
| **NotificationController** | `/api/notifications/*` | Notifications |

#### Services
- **WhisperSpeechToTextService** - Integrates with Python STT microservice
- **EventService** - Event creation, filtering, retrieval
- **RecommendationService** - Generates recommendations via rule engine
- **RecommendationRuleEngine** - Business rules for recommendations
- **TimelineService** - Aggregates events into timeline view
- **UserService** - User registration, profile management
- **CalendarService** - Calendar event management
- **NotificationService** - Notification handling
- **LifeAiService** - AI integration

#### Database Schema
| Table | Purpose |
|-------|---------|
| `users` | User accounts and profiles |
| `events` | Health events (food, exercise, sleep, etc.) |
| `recommendations` | Generated recommendations for users |
| `timelines` | Timeline aggregations |
| `calendars` | Calendar entries |
| `notifications` | System notifications |

**Development:**
```bash
cd lifeai-backend
mvn clean install
mvn spring-boot:run
# API: http://localhost:8080/api
```

**Build:**
```bash
mvn clean package  # Production JAR
java -jar target/lifeai-backend-1.0.0-SNAPSHOT.jar
```

---

### 3. STT Microservice (Python)
**Location:** `lifeai-stt/`

**Features:**
- **100% Offline** - No cloud dependencies
- **Multi-language** - Polish, English, and 99+ languages
- **Optimized** - INT8 quantization for CPU efficiency
- **Fast** - 1-3 seconds per audio file (after initial load)

**API Endpoints:**
```http
POST /transcribe
Content-Type: multipart/form-data

Parameters:
- file: audio file (WAV, MP3, M4A, OGG, WEBM)
- language: optional (default: pl)

Response:
{
  "text": "Transcribed text",
  "language": "pl",
  "confidence": 0.95,
  "processing_time_ms": 2340,
  "model": "faster-whisper"
}
```

**Development:**
```bash
cd lifeai-stt
python -m venv venv
source venv/bin/activate  # or venv\Scripts\activate on Windows
pip install -r requirements.txt
python -m uvicorn app.main:app --host 0.0.0.0 --port 5000 --reload
# API: http://localhost:5000
```

---

## 🔄 Data Flow: Audio to Recommendation

1. **Frontend captures audio** → WAV/MP3/M4A format
2. **Sends to Backend** → POST `/api/speech-to-text/transcribe`
3. **Backend forwards to STT** → POST `http://localhost:5000/transcribe`
4. **STT processes audio** → Faster-Whisper model executes
5. **Returns transcription** → JSON with text, language, confidence
6. **Backend creates Event** → Detects type from transcribed text
7. **Generates Recommendations** → Rule engine analyzes event
8. **Returns complete response** → Transcription + Event + Recommendations
9. **Frontend displays results** → User sees event in timeline with suggestions

---

## 🛠️ Getting Started

### Prerequisites
- **Node.js** 16+ (Frontend)
- **Java 21** & **Maven 3.6+** (Backend)
- **Python 3.10+** (STT)
- **PostgreSQL 12+** (Database)
- **Docker** & **Docker Compose** (Optional)

### Quick Setup

#### Option 1: Using Docker Compose (Recommended)
```bash
# Copy environment file
cp lifeai-stt/.env.example lifeai-stt/.env

# Start all services
docker-compose up

# Services available at:
# Frontend: http://localhost:3000
# Backend: http://localhost:8080/api
# STT: http://localhost:5000
# Database: localhost:5432
```

#### Option 2: Manual Setup

**1. Database:**
```bash
# Start PostgreSQL
docker run --name postgres-lifeai \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:15

# Create databases
createdb -h localhost -U postgres lifeai
createdb -h localhost -U postgres lifeai_dev
```

**2. STT Microservice:**
```bash
cd lifeai-stt
python -m venv venv
source venv/bin/activate
pip install -r requirements.txt
python -m uvicorn app.main:app --host 0.0.0.0 --port 5000
```

**3. Backend:**
```bash
cd lifeai-backend
mvn clean install
mvn spring-boot:run
```

**4. Frontend:**
```bash
cd lifeai-frontend
npm install
npm run dev
```

---

## 📝 Configuration

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

stt:
  service-url: http://localhost:5000
  default-language: pl
  connect-timeout: 30000
  read-timeout: 300000

server:
  port: 8080
```

### STT Microservice (.env)
```
APP_NAME=LifeAI Speech-to-Text Service
WHISPER_MODEL_SIZE=base
LOG_LEVEL=INFO
DEBUG=False
```

### Frontend (.env)
```
VITE_API_URL=http://localhost:8080
VITE_APP_NAME=LifeAI
VITE_APP_ENV=development
```

---

## ✨ Key Features

### Health Event Tracking
- Record daily health events (food, exercise, sleep, stress, etc.)
- Audio-based input via speech-to-text
- Metadata and timestamp tracking

### Intelligent Recommendations
- Rule-based recommendation engine
- Context-aware suggestions
- Priority-based notifications
- Track applied recommendations

### Timeline & Calendar
- Visual timeline of health events
- Calendar integration for important events
- Event statistics and aggregation
- Date range filtering

### User Management
- User registration and authentication
- Profile management
- User preferences
- Activity history

### AI Integration
- AI-powered insights (extensible)
- Pattern recognition in health data
- Personalized suggestions

---

## 🧪 Testing

### Backend Tests
```bash
cd lifeai-backend
mvn test  # Run unit and integration tests
```

Tests use TestContainers for PostgreSQL, ensuring proper database operation without external dependencies.

### Frontend Linting
```bash
cd lifeai-frontend
npm run lint  # ESLint checks
```

---

## 🐳 Docker Deployment

### Build Images
```bash
# Frontend
cd lifeai-frontend
docker build -t lifeai-frontend .

# Backend
cd lifeai-backend
docker build -t lifeai-backend .

# STT
cd lifeai-stt
docker build -t lifeai-stt .
```

### Run with Docker Compose
```bash
docker-compose up --build
```

---

## 📚 API Examples

### Create Health Event
```bash
POST /api/events
Content-Type: application/json

{
  "userId": 1,
  "eventType": "FOOD",
  "description": "Ate a large pasta meal",
  "metadata": {
    "portion": "large",
    "cuisine": "italian"
  }
}
```

### Transcribe Audio
```bash
POST /api/speech-to-text/transcribe
Content-Type: multipart/form-data

file: <audio_file.wav>
language: pl
userId: 1 (optional - auto-creates event)
```

### Get Recommendations
```bash
GET /api/recommendations?userId=1&priority=HIGH
```

### Get Timeline
```bash
GET /api/timeline?userId=1&startDate=2025-12-01&endDate=2025-12-31
```

---

## 🔐 Security Considerations

- ✅ PostgreSQL database with strong credentials
- ⚠️ **TODO**: Implement password hashing (bcrypt/argon2)
- ⚠️ **TODO**: Add JWT authentication
- ⚠️ **TODO**: Implement CORS policies for production
- ✅ Exception handling for error responses
- ✅ Validation via Jakarta Bean Validation

---

## 📖 Additional Documentation

- **[ARCHITECTURE.md](./ARCHITECTURE.md)** - Detailed system architecture and design decisions
- **[lifeai-frontend/README.md](./lifeai-frontend/README.md)** - Frontend-specific documentation
- **[lifeai-stt/README.md](./lifeai-stt/README.md)** - STT microservice documentation
- **[lifeai-backend/](./lifeai-backend/)** - Backend source code and configuration

---

## 🚧 Future Enhancements

### Frontend
- [ ] User authentication and login
- [ ] Dark mode toggle
- [ ] Real-time notifications
- [ ] Data visualization (charts, graphs)
- [ ] Offline support (PWA)

### Backend
- [ ] JWT-based authentication
- [ ] Advanced recommendation ML models
- [ ] Integration with health APIs (Fitbit, Apple Health)
- [ ] Batch processing and analytics
- [ ] WebSocket support for real-time updates

### STT Microservice
- [ ] Async processing queue
- [ ] Real-time streaming transcription
- [ ] GPU support (CUDA/cuDNN)
- [ ] Batch processing API
- [ ] Custom model fine-tuning

---

## 📞 Support

For issues, questions, or contributions, please refer to:
- Backend issues: Check `lifeai-backend/` directory
- Frontend issues: Check `lifeai-frontend/` directory
- STT issues: Check `lifeai-stt/` directory

---

## 📄 License

MIT License - See individual component directories for details.

---

**Last Updated:** December 2025  
**Version:** 1.0.0
