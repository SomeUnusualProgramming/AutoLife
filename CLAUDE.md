# LifeAI - Quick Reference for Developers

## 🚀 Quick Start Commands

### Docker Compose (Recommended)
```bash
# Start all services (will take 2-3 minutes for first run)
docker-compose up -d

# View logs
docker-compose logs -f stt       # Python STT service
docker-compose logs -f backend   # Java Spring Boot
docker-compose logs -f frontend  # React app
docker-compose logs -f postgres  # Database

# Stop all
docker-compose down

# Full reset (wipe database)
docker-compose down -v
```

### Local Development (4 Terminals)

**Terminal 1: PostgreSQL**
```bash
docker run --name postgres-lifeai -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=lifeai -p 5432:5432 postgres:16-alpine
```

**Terminal 2: STT Service (Python)**
```bash
cd lifeai-stt
python -m venv venv
# Windows: venv\Scripts\activate
# Mac/Linux: source venv/bin/activate
pip install -r requirements.txt
python -m uvicorn app.main:app --host 0.0.0.0 --port 5000 --reload
```

**Terminal 3: Backend (Java)**
```bash
cd lifeai-backend
mvn clean package -DskipTests
java -Dspring.profiles.active=local -jar target/lifeai-backend-1.0.0-SNAPSHOT.jar
# OR in IDE: Run LifeAiBackendApplication.java
```

**Terminal 4: Frontend (React)**
```bash
cd lifeai-frontend
npm install
npm run dev
# Opens at http://localhost:5173
```

---

## 📍 Service URLs

| Service | URL | Port |
|---------|-----|------|
| Frontend | http://localhost:3000 | 3000 |
| Backend API | http://localhost:8080 | 8080 |
| STT Service | http://localhost:5000 | 5000 |
| Database | localhost | 5432 |

---

## ✅ Health Checks

```bash
# All services should respond
curl http://localhost:3000        # Frontend (HTML)
curl http://localhost:8080/api/health # Backend (note /api prefix)
curl http://localhost:5000/health # STT Service
```

---

## 📁 Project Structure

```
AutoLife/
├── lifeai-frontend/      # React - Vite + Tailwind
│   └── src/
│       ├── components/   # AudioRecorder, Dashboard, Timeline, Calendar, SkeletonLoader
│       ├── hooks/        # useAudioRecorder, useSpeechToText, useTimeline, useRecommendations
│       ├── services/     # api.ts (axios client with transformers)
│       └── types/        # TypeScript interfaces
│
├── lifeai-backend/       # Spring Boot 3.2 + Java 21
│   └── src/main/java/com/lifeai/
│       ├── controller/   # SpeechToTextController, EventController, etc.
│       ├── service/      # WhisperSpeechToTextService, EventService, etc.
│       ├── entity/       # User, Event, Recommendation, Timeline, Calendar
│       ├── repository/   # Database access
│       ├── dto/          # Request/Response models
│       ├── recommendation/ # RuleEngine
│       └── config/       # Configuration
│
├── lifeai-stt/           # Python FastAPI + Faster-Whisper
│   └── app/
│       ├── main.py       # FastAPI app, routes, lifespan management
│       ├── services.py   # WhisperService (transkrypcja)
│       ├── models.py     # Pydantic models (request/response)
│       └── config.py     # Settings management
│
├── docker-compose.yml    # Orchestration (postgres, stt, backend, frontend)
├── ARCHITECTURE.md       # Full system architecture
├── PROJECT_PLAN.md       # Complete project plan with all details
└── CLAUDE.md            # This file
```

---

## 🔌 Key Interfaces

### Speech-to-Text Pipeline
```
Frontend (React)
  ↓ POST /api/speech-to-text/transcribe (multipart/form-data)
Backend (Spring Boot)
  ↓ WhisperSpeechToTextService calls
STT Service (Python)
  ↓ POST /transcribe
Faster-Whisper Model
  ↓ Returns { text, language, confidence, processing_time_ms }
```

### Event Creation
```
SpeechToTextController
  → transcribeAudio()
  → createEventFromTranscription()
    → detectEventType() [regex-based: "jad", "ćwicz", etc.]
    → EventService.createEvent()
    → RecommendationService.generateRecommendationsForEvent()
      → RuleEngine.analyzeEvent()
      → Creates Recommendation(s)
```

---

## 📝 Common Tasks

### Add New Endpoint (Backend)

1. Create **Controller** (`SpeechToTextController.java`)
   ```java
   @RestController
   @RequestMapping("/api/your-resource")
   public class YourController {
       @PostMapping("/action")
       public ResponseEntity<?> action(@RequestBody YourRequest req) {
           // Logic here
       }
   }
   ```

2. Create **Service** (`YourService.java`)
   ```java
   @Service
   @Slf4j
   public class YourService {
       public void doSomething() { }
   }
   ```

3. Create **Entity** (`YourEntity.java`)
   ```java
   @Entity
   @Table(name = "your_table")
   public class YourEntity extends BaseEntity {
       // Fields with @Column, @ManyToOne, etc.
   }
   ```

4. Create **Repository** (`YourRepository.java`)
   ```java
   @Repository
   public interface YourRepository extends JpaRepository<YourEntity, Long> {
       List<YourEntity> findByUserId(Long userId);
   }
   ```

### Add New React Component

1. Create component (`src/components/YourComponent.tsx`)
   ```tsx
   import { useState } from 'react'
   
   interface YourComponentProps {
     onAction?: (data: any) => void
   }
   
   export const YourComponent = ({ onAction }: YourComponentProps) => {
     const [state, setState] = useState('')
     
     return (
       <div className="...">
         {/* JSX here */}
       </div>
     )
   }
   ```

2. Add to `src/components/index.ts`
   ```ts
   export { YourComponent } from './YourComponent'
   ```

3. Use in other components
   ```tsx
   import { YourComponent } from '../components'
   ```

### Add Python Dependency

```bash
cd lifeai-stt
source venv/bin/activate  # or venv\Scripts\activate on Windows
pip install <package-name>
pip freeze > requirements.txt
```

---

## 🧪 Testing

### Backend Tests
```bash
cd lifeai-backend
# Run all tests
mvn test

# Run specific test
mvn test -Dtest=SpeechToTextControllerTest

# Run with coverage
mvn clean test jacoco:report
```

### Frontend Tests
```bash
cd lifeai-frontend
npm test
npm run test:coverage
```

### STT Service Tests
```bash
cd lifeai-stt
python -m pytest
python -m pytest -v  # Verbose
```

---

## 🔍 Debugging

### Backend Logging
- Set `logging.level.com.lifeai: DEBUG` in `application.yml`
- Check logs: `docker-compose logs -f backend`

### STT Logs
- Set `LOG_LEVEL=DEBUG` in `.env`
- Logs print to console

### Database Inspection
```bash
# Connect to PostgreSQL
docker-compose exec postgres psql -U postgres -d lifeai

# Useful queries
SELECT * FROM events;
SELECT * FROM recommendations;
SELECT COUNT(*) FROM users;
DESC events;  -- Show table structure
```

### Frontend Console
- Open browser DevTools (F12)
- Console tab for errors
- Network tab to see API calls to backend

---

## 🏗️ Architecture Overview

### Backend Communication
- **Frontend → Backend**: REST API on `http://localhost:8080`
- **Backend → STT**: HTTP calls to `http://localhost:5000/transcribe`
- **Backend → Database**: JDBC over TCP to PostgreSQL on 5432

### Key Configuration Files
- `lifeai-backend/src/main/resources/application.yml` - Spring config
- `lifeai-stt/app/config.py` - Python settings
- `docker-compose.yml` - Docker orchestration
- `.env` - Environment variables for docker-compose

### Event Types (detected automatically from text)
- `FOOD` - "jad", "jadł", "posiłek", "sniadani", "obiad", "kolacj"
- `ACTIVITY` - "aktywno", "ćwicz", "trening", "spacer", "sport"
- `SLEEP` - "sen", "spał"
- `MOOD` - "humor", "nastrój", "czuj"
- `SYMPTOM` - "objaw", "ból", "gorączk"
- `MEDICATION` - "lekarst", "medycyn"
- `DOCTOR_VISIT` - "lekarz", "doktor", "wizyta"
- `WEIGHT` - "wag", "kilogram"
- `OTHER` - default

---

## 📊 Performance Notes

| Operation | Time | Notes |
|-----------|------|-------|
| Model load (first) | 5-10s | Cached after first load |
| Short audio (<30s) | 1-3s | Depends on model size |
| Long audio (5min) | 5-15s | Linear with duration |
| Event creation | <500ms | DB insert |
| Recommendation gen | <1s | RuleEngine execution |
| Frontend load | <2s | Vite optimized |

---

## 🚨 Common Issues

### "STT service is not initialized"
- Check Python service is running: `curl http://localhost:5000/health`
- Check STT_SERVICE_URL is set correctly in backend

### "Connection refused" (Backend → STT)
- In Docker: use `http://stt:5000` (service name from docker-compose)
- Locally: use `http://localhost:5000`

### "Model download fails"
- First run needs internet to download ~140MB Whisper model
- Model cached in `~/.cache/huggingface`
- Set `WHISPER_MODEL_SIZE=tiny` for faster testing

### "Frontend can't reach backend"
- Check VITE_API_URL in docker-compose.yml
- Backend must be on same network or accessible from frontend

### Database migration issues
- Check `SPRING_JPA_HIBERNATE_DDL_AUTO=update` in docker-compose
- Or manually run migrations if issues persist

---

## 📚 Key Files Reference

| File | Purpose |
|------|---------|
| `docker-compose.yml` | Service orchestration - **edit here to add services** |
| `PROJECT_PLAN.md` | Complete documentation (READ THIS!) |
| `ARCHITECTURE.md` | System design and data flow |
| `lifeai-backend/pom.xml` | Java dependencies |
| `lifeai-stt/requirements.txt` | Python dependencies |
| `lifeai-frontend/package.json` | Node.js dependencies |

---

## 🆕 Latest Implementation (Dec 2025)

### New Components Added

**SkeletonLoader** (`src/components/SkeletonLoader.tsx`)
- Reusable animated skeleton loading component
- Supports two types: `timeline` and `recommendations`
- Smooth CSS animations while data is loading
- Configurable count parameter for multiple skeletons

**Calendar** (`src/components/Calendar.tsx`)
- Month view calendar with event indicators
- Shows event counts as badges on days
- Today indicator (emerald styling)
- Interactive navigation (previous/next month)
- Polish weekday labels
- Legend showing event status

### Data Flow Architecture

**Timeline**: Backend → Frontend Transformation
```
Backend Response: { days: [...], totalEvents: N }
                 ↓ (transformTimelineResponse)
Frontend State:  { entries: [...], total: N }
                 ↓ (useTimeline hook)
Component:       Timeline (displays grouped events)
```

**Recommendations**: Field Mapping
```
Backend Response: { text, type, priority, status, isApplied }
                 ↓ (transformRecommendationResponse)
Frontend State:  { suggestion, category, priority, status, aiGenerated }
                 ↓ (useRecommendations hook)
Component:       AI Suggestions (displays top 5)
```

### Key Features Implemented

- ✅ **Auto-fetching**: Timeline & Recommendations load when user is ready
- ✅ **Data mapping**: Backend response → Frontend types transformer
- ✅ **Skeleton loaders**: Visual feedback during data fetch
- ✅ **Empty states**: Helpful messages with icons and guidance
- ✅ **Calendar integration**: Interactive month view with event badges
- ✅ **UserId guards**: Prevents fetching before user is initialized

### Files Modified

| File | Changes |
|------|---------|
| `src/services/api.ts` | Added transformTimelineResponse() & transformRecommendationResponse() |
| `src/hooks/index.ts` | Added userId null checks in useTimeline & useRecommendations |
| `src/components/Dashboard.tsx` | Added Calendar, improved UX, auto-fetch triggers |
| `src/components/Timeline.tsx` | Replaced spinner with SkeletonLoader, enhanced empty state |
| `src/components/SkeletonLoader.tsx` | ✨ New component (180 lines) |
| `src/components/Calendar.tsx` | ✨ New component (180 lines) |
| `src/components/index.ts` | Added exports for SkeletonLoader & Calendar |

### Build Status

```
✓ 93 modules transformed
✓ Built in ~1.2s
✓ 0 TypeScript errors
✓ Production optimized (~3KB increase)
```

---

## 🎯 Next Steps

1. **Run docker-compose** to start all services (already running)
2. **Open frontend** at http://localhost:3000
3. **Test features**:
   - Record audio → Event appears in Timeline + Calendar
   - Add text event → Same behavior
   - Calendar shows event indicators
   - AI Suggestions load automatically
4. **Check logs** if anything fails
5. **Read PROJECT_PLAN.md** for detailed architecture

---

## 📞 Support Commands

```bash
# Check all containers running
docker-compose ps

# View container logs (last 50 lines, follow new)
docker-compose logs -f [service-name] --tail=50

# Execute command in container
docker-compose exec [service-name] [command]
# Example:
docker-compose exec backend java -version
docker-compose exec stt python --version
docker-compose exec postgres psql -U postgres -d lifeai

# Restart service
docker-compose restart [service-name]

# Remove everything and start fresh
docker-compose down -v
docker-compose up -d
```

---

**Status**: ✅ Production Ready
**Version**: 1.1.0 (Timeline + Calendar + UX Improvements)
**Last Updated**: December 17, 2025
**Latest Changes**: 
- Timeline/Recommendations data mapping (Phase 1)
- Auto-fetching with userId guards (Phase 2)
- Skeleton loaders + empty states (Phase 3)
- Calendar integration (Phase 4)
