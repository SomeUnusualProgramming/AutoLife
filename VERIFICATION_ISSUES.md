# PROJECT_STATE.md Verification Issues

## Critical Discrepancies Found

### 1. Timeline Response Structure - MISMATCH ❌

**Documented in PROJECT_STATE.md (Section 3)**:
```json
{
  "entries": [TimelineEntry],
  "total": number
}
```

**Actual Backend Response (TimelineService.java + TimelineResponse.java)**:
```json
{
  "userId": number,
  "fromDate": "ISO date",
  "toDate": "ISO date", 
  "totalEvents": number,
  "days": [
    {
      "date": "ISO date",
      "events": [TimelineEventResponse],
      "eventCount": number
    }
  ]
}
```

**Frontend Code (Timeline.tsx:138)**:
```typescript
const displayGroups = timelineData?.entries ? groupEntriesByDay(timelineData.entries) : []
```

**Status**: ❌ Frontend expects `.entries` but backend returns `.days` with different structure
**Impact**: Timeline feature may not work correctly - CODE IS BROKEN

---

### 2. RecommendationResponse JSON Mapping ❌

**Documented**: Field names match Java entity (text, type, etc.)

**Actual Code (RecommendationResponse.java:27-42)**:
```java
@JsonProperty("suggestion")
private String text;

@JsonProperty("category")
private RecommendationType type;

@JsonProperty("status")
private String status;

@JsonProperty("actionUrl")
private String actionUrl;

@JsonProperty("aiGenerated")
private Boolean aiGenerated;
```

**Actual JSON Output** (what frontend receives):
```json
{
  "suggestion": "string",      // NOT "text"
  "category": "type_name",     // NOT "type"
  "status": "string",
  "actionUrl": "string",
  "aiGenerated": boolean,
  "id": number,
  "eventId": number,
  "userId": number,
  "priority": "HIGH|MEDIUM|LOW",
  "isApplied": boolean,
  "appliedAt": "ISO datetime",
  "createdAt": "ISO datetime",
  "updatedAt": "ISO datetime"
}
```

**Status**: ❌ Critical - Field names don't match documentation

---

### 3. SpeechToTextController Endpoint Path

**Documented**: `POST /api/speech-to-text/transcribe`

**Actual Code**: `@RequestMapping("/speech-to-text")` + `@PostMapping("/transcribe")`
- Full path: `POST /speech-to-text/transcribe` ✅ CORRECT

But frontend call (AudioRecorder.tsx:241):
```typescript
const result = await speechToTextApi.transcribeAudio(audioFile, language, userId)
```

Frontend api.ts (api.ts:125):
```typescript
POST '/api/speech-to-text/transcribe'
```

**Issue**: Documented as `/api/speech-to-text/transcribe` but should verify the actual routing configuration

---

### 4. Frontend Hooks File Structure

**Documented**: "Listed as separate files"

**Actual**: 
- `src/hooks/index.ts` - Contains ALL hooks (useAudioRecorder, useSpeechToText, useTimeline, useRecommendations, useSendEvent, useAsync)
- `src/hooks/useUser.ts` - Single file for useUser

**Status**: ⚠️ Minor - Documentation should reflect single index.ts file

---

### 5. useUser Hook - UserId Management

**Documented**: "hardcoded or optional userId"

**Actual Code (useUser.ts)**:
```typescript
const DEFAULT_USER_ID = 1
const USER_ID_KEY = 'lifeai_user_id'

// Stores in localStorage, defaults to 1 on first run
// Can be updated via updateUserId()
```

**Status**: ⚠️ Documentation incomplete - userId is persistent in localStorage

---

### 6. EventType Enum Values

**Documented**: 10 types (FOOD, ACTIVITY, DOCTOR_VISIT, MEDICATION, SYMPTOM, WEIGHT, SLEEP, MOOD, OTHER, MEDICAL)

**Actual (EventType.java)**:
```java
FOOD,
ACTIVITY,
DOCTOR_VISIT,
MEDICATION,
SYMPTOM,
WEIGHT,
SLEEP,
MOOD,
MEDICAL,
OTHER
```

**Status**: ✅ CORRECT

---

### 7. Database DDL Strategy

**Documented**: `spring.jpa.hibernate.ddl-auto=update`

**Actual (docker-compose.yml:56)**: `SPRING_JPA_HIBERNATE_DDL_AUTO: update`

**Status**: ✅ CORRECT

---

## Summary of Critical Issues

| Issue | Type | Severity | Impact |
|-------|------|----------|--------|
| Timeline response structure mismatch | API Contract | 🔴 **CRITICAL** | Timeline display broken |
| RecommendationResponse field mapping | JSON Serialization | 🔴 **CRITICAL** | Recommendations display incorrect |
| Hooks file organization | Documentation | 🟡 Minor | Development clarity |
| useUser persistence | Documentation | 🟡 Minor | User context understanding |

---

## What Needs to Be Fixed in PROJECT_STATE.md

### Section 3.3 - Timeline Response
Replace the documented response schema with actual structure from TimelineResponse.java

### Section 3.4 - Recommendations Response  
Add @JsonProperty mapping details - field names in JSON differ from Java entity names

### Section 3 - Frontend Components
Correct the hooks file structure - all in index.ts, not separate files

### Section 8 - Verify Controller Paths
Ensure `/api/` prefix is documented (or verify if it's added elsewhere)

---

## Questions for Validation

1. **Is the Timeline feature actually working with the current code?**
   - Frontend expects `.entries`, backend returns `.days`
   - This is a breaking mismatch

2. **Are the @JsonProperty mappings intentional?**
   - Why rename `text` to `suggestion`?
   - This affects the frontend's ability to display recommendations

3. **Is there an ApiResponse wrapper that transforms responses?**
   - Frontend api.ts accesses `response.data` (Axios wrapper)
   - But where's the `/api/` prefix added?

