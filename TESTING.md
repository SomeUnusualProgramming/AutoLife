# LifeAI - Testing Guide & MVP Checklist

## 🚀 Quick Start (Lokalnie)

### Wymagania
- PostgreSQL 15+ uruchomiony na `localhost:5432`
- Java 21+ (Spring Boot)
- Python 3.9+ (Faster-Whisper STT)
- Node.js 18+ (Frontend)

### 1️⃣ Start PostgreSQL
```bash
docker run --name postgres-lifeai \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:15
```

### 2️⃣ Start STT Microservice (Python)
```bash
cd lifeai-stt
pip install -r requirements.txt
python -m uvicorn app.main:app --host 0.0.0.0 --port 5000
```

Powinno wyświetlić:
```
INFO:     Uvicorn running on http://0.0.0.0:5000
INFO:     Application startup complete
```

**Test STT:**
```bash
curl -X POST "http://localhost:5000/health"
# Response: {"status":"healthy","model":"faster-whisper","version":"0.10.0"}
```

### 3️⃣ Start Backend (Spring Boot)
```bash
cd lifeai-backend
mvn spring-boot:run
```

Powinno wyświetlić:
```
INFO com.lifeai.LifeAiBackendApplication - Starting LifeAI Backend...
INFO com.lifeai.config.DataInitializer - Created default user: id=1, email=user@lifeai.local
INFO org.springframework.boot.web.embedded.tomcat.TomcatWebServer - Tomcat started on port(s): 8080
```

### 4️⃣ Start Frontend (React)
```bash
cd lifeai-frontend
npm install
npm run dev
```

Otwórz: http://localhost:5173/

---

## 📋 MVP Checklist - Co Działa

### ✅ Whisper STT (Speech-to-Text)
- [x] Nagrywanie audio (Web Audio API)
- [x] Wysłanie do STT microservice
- [x] Transkrypcja tekstu (Faster-Whisper)
- [x] Odpowiedź zwraca: `transcription`, `event`, `recommendations`
- [x] Debug logi na każdym etapie

**Test manualne:**
1. Kliknij "Start Recording" 
2. Mów: "Dzisiaj jadłem porcję makaronu"
3. Kliknij "Stop"
4. Wciśnij "Transcribe"
5. Powinny pojawić się: transkrypcja, event type (FOOD), rekomendacje

### ✅ Event Creation (Backend → Database)
- [x] Automatyczne tworzenie Event z transkrypcji
- [x] Detekcja typu zdarzenia (FOOD, ACTIVITY, SLEEP, MOOD, SYMPTOM, itd.)
- [x] Zapis do PostgreSQL
- [x] Metadane (source, textLength, createdAt)
- [x] Default user (id=1) auto-tworzy się na starcie

**Test REST API:**
```bash
# Transkrypcja + Event Creation
curl -X POST "http://localhost:8080/api/speech-to-text/transcribe?userId=1&language=pl" \
  -F "file=@audio.wav"

# Response ma: transcription, event, recommendations
```

### ✅ Recommendations (Rule Engine)
- [x] Reguła: FOOD (słodycze) → PHYSICAL_ACTIVITY
- [x] Reguła: ACTIVITY (intensywna) → SLEEP_IMPROVEMENT
- [x] Reguła: SLEEP (zbyt mało) → SLEEP_IMPROVEMENT (HIGH priority)
- [x] Reguła: MOOD (negatywny) → PHYSICAL_ACTIVITY (HIGH priority)
- [x] Reguła: SYMPTOM (poważny) → DOCTOR_VISIT (HIGH priority)
- [x] Zwracane w response z transkrypcji
- [x] Możliwość pobierania per userId

**Test REST API:**
```bash
# Pobierz rekomendacje dla userId=1
curl -X GET "http://localhost:8080/api/recommendations?userId=1"

# Response: Lista RecommendationResponse z text, type, priority
```

### ✅ Timeline (Historia zdarzeń)
- [x] Pobieranie zdarzeń po userId
- [x] Grupowanie po dacie
- [x] Sortowanie chronologiczne (najnowsze pierwsz)
- [x] Frontend odświeża po nowym evencie z transkrypcji

**Test REST API:**
```bash
curl -X GET "http://localhost:8080/api/timeline?userId=1&from=2025-12-01&to=2025-12-31"

# Response: TimelineResponse z days, eventCount
```

### ✅ Frontend
- [x] LocalStorage userId (default=1)
- [x] Wysyłanie userId do wszystkich API calls
- [x] AudioRecorder z callbackami (onEventCreated, onRecommendationsReceived)
- [x] Wyświetlanie rekomendacji z transkrypcji bezpośrednio
- [x] Odświeżanie Timeline po transkrypcji
- [x] Debug logi w konsoli przeglądarki

---

## 📊 Co Jeszcze Jest Mockiem/Placeholderem

- ❌ **Autentykacja** - userId=1 hardcoded/LocalStorage, brak JWT
- ❌ **User Registration** - Brak flow rejestracji w UI
- ❌ **Advanced Recommendations** - Tylko 5 reguł, bez Machine Learning
- ❌ **User Preferences** - Brak personalizacji (dieta, alergeny, itp.)
- ❌ **Notification System** - Brak push/email powiadomień
- ❌ **Calendar Integration** - Brak integracji z kalendarzem
- ❌ **Data Export** - Brak eksportu do CSV/PDF
- ❌ **Mobile App** - Tylko web (React)

---

## 🧪 Testowanie End-to-End

### Szenariusz 1: Audio → Event → Recommendations
**Cel:** Sprawdzić kompletny flow transkrypcji

1. **Frontend:** Nagrywanie
   - Otwórz http://localhost:5173
   - Wciśnij "Start Recording"
   - Mów: "Dziś jadłem dużo słodyczy, całą czekoladę"
   - Wciśnij "Stop"

2. **Frontend:** Transkrypcja
   - Wciśnij "Transcribe" lub czekaj auto-transkrypcji
   - Powinno wyświetlić transkrypcję w polu tekstowym

3. **Frontend:** Event + Recommendations
   - Powinny pojawić się w panelu "AI Suggestions" na prawo
   - Recommendation powinien mówić o fizycznej aktywności

4. **Backend Logs:**
   ```
   INFO: Transcribing audio file...
   INFO: Created event: id=X, type=FOOD, userId=1
   INFO: Generated 1 recommendations for event id: X
   ```

5. **Database Check:**
   ```sql
   SELECT * FROM events WHERE user_id = 1 ORDER BY timestamp DESC LIMIT 1;
   SELECT * FROM recommendations WHERE user_id = 1 ORDER BY created_at DESC LIMIT 1;
   ```

### Szenariusz 2: Manual Event + Timeline Update
**Cel:** Sprawdzić flow bez transkrypcji

1. **Frontend:** Dodaj event
   - W polu tekstowym napisz: "Szybki spacer 20 minut"
   - Wciśnij "Add Event" lub Ctrl+Enter

2. **Frontend:** Timeline
   - Event powinien pojawić się w "Dzienniku dnia"
   - Powinien być oznaczony jako zdrowy (zielony kolor)

3. **Backend Logs:**
   ```
   INFO: Received event from user...
   ```

### Szenariusz 3: Różne Event Types
**Cel:** Sprawdzić detekcję typu zdarzenia

```
Text                           → Type          → Recommendation
"Mało spałem"                  → SLEEP         → SLEEP_IMPROVEMENT (HIGH)
"Czuję się smutny"             → MOOD          → PHYSICAL_ACTIVITY (HIGH)
"Gorączka 39°C"                → SYMPTOM       → DOCTOR_VISIT (HIGH)
"Poszedłem na intensywny bieg" → ACTIVITY      → SLEEP_IMPROVEMENT
"Zjadłem lody"                 → FOOD          → PHYSICAL_ACTIVITY
```

---

## 🔍 Debug & Logi

### Frontend Console (Browser DevTools)
```javascript
// Otwórz DevTools (F12) → Console

// Powinny być logi:
// "Event created from transcription: {...}"
// "Recommendations received from transcription: [...]"
// "Fetching timeline with userId..."
```

### Backend Logs (Terminal)
```
Szukaj linijek zawierających:
- "Transcribing audio file"
- "Created event: id=..."
- "Generated N recommendations"
- "DataInitializer: Created default user"
```

### STT Logs (Python Terminal)
```
INFO:     POST /transcribe
INFO:     Transcription completed in XXXms
```

### Database Check
```bash
# PostgreSQL
psql -U postgres -d lifeai -c "SELECT * FROM events ORDER BY timestamp DESC LIMIT 5;"
psql -U postgres -d lifeai -c "SELECT * FROM recommendations ORDER BY created_at DESC LIMIT 5;"
psql -U postgres -d lifeai -c "SELECT * FROM users;"
```

---

## 🐛 Troubleshooting

### "Service unavailable" przy transkrypcji
- [ ] Sprawdź czy STT (Python) jest uruchomiony na porcie 5000
- [ ] `curl http://localhost:5000/health`
- [ ] Sprawdź Backend logs czy kontaktuje się ze STT

### Brak rekomendacji po transkrypcji
- [ ] Sprawdź czy event został utworzony (Backend logs)
- [ ] Sprawdź czy tekst zawiera słowa kluczowe reguł
- [ ] Database: `SELECT * FROM recommendations WHERE event_id = X;`

### Timeline jest pusty
- [ ] Sprawdź userId w request (powinien być 1)
- [ ] Sprawdź czy events istnieją w DB: `SELECT COUNT(*) FROM events WHERE user_id=1;`

### Audio nie nagrywa
- [ ] Sprawdź permisje mikrofonu (przeglądarki)
- [ ] Sprawdź czy `navigator.mediaDevices.getUserMedia` działa
- [ ] Browser console: sprawdź błędy

---

## 📈 Performance Notes

- **STT Processing Time:** ~2-5 sekund dla 30s audio (zależy od CPU/GPU)
- **Event Creation:** <100ms (database write)
- **Recommendations Generation:** <50ms (rule engine)
- **Timeline Load:** <200ms dla 30 zdarzeń

---

## 🎯 Co Testować Przed Deploy

- [ ] Audio recording działa w Chrome, Firefox, Edge
- [ ] Transkrypcja tekstu jest dokładna (min 90%)
- [ ] Event type detection jest poprawny dla głównych keywords
- [ ] Rekomendacje są sensowne (nie sprzeczne)
- [ ] Timeline odświeża po nowym evencie
- [ ] Baza danych nie się sypie przy wielu eventach
- [ ] Responsive design (mobile, tablet, desktop)

---

## 📝 Notes

- Default user (userId=1) tworzy się automatycznie na starcie Backendu
- Wszystkie timestamps są w UTC (database) i konwertują się na lokalny czas na frontendu
- Rekomendacje mogą się duplikować jeśli event ma cechy kilku reguł (to jest OK)
- Whisper model (base 140MB) jest załadowywany na startup STT - czeka ~10-30s

---

**Stan MVP:** ✅ Ready for local testing
**Data:** 2025-12-17
