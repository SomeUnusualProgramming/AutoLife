# Technical Specification: Timeline Rendering Alignment (MVP Fix)

**Document Version**: 1.0  
**Date**: December 17, 2025  
**Scope**: Frontend-only changes; no backend modifications  
**Status**: Ready for Implementation

---

## 1. Executive Summary

The Timeline component fails to display events because of a field name mismatch between backend response and frontend type expectations:

- **Backend sends**: `{ userId, fromDate, toDate, totalEvents, days: [...] }`
- **Frontend expects**: `{ entries: [...], total: number }`

This Tech Spec defines the minimal frontend-only changes required to align the UI with the actual backend contract.

**Key Constraint**: No backend API changes. The backend response structure is the source of truth.

---

## 2. Architecture Overview

### Data Flow: Timeline Rendering Pipeline

```
Backend Timeline API
    ↓ (TimelineResponse: { days: [...] })
axios request (services/api.ts:timelineApi.getTimeline)
    ↓ (raw response data)
useTimeline hook (hooks/index.ts:65-95)
    ↓ (stores in state as `data`)
Timeline component (components/Timeline.tsx:136)
    ↓ (accesses data?.entries [CURRENTLY BROKEN] or data?.days [TO FIX])
Rendering logic (grouping, filtering, display)
    ↓ (user sees events or "Brak zdarzeń")
```

### Current Broken State

```typescript
// Timeline.tsx:138 (BROKEN)
const displayGroups = timelineData?.entries ? groupEntriesByDay(timelineData.entries) : []
// Result: timelineData.entries is ALWAYS undefined
// Rendering: Always shows empty state "Brak zdarzeń"
```

### Fixed State (Target)

```typescript
// Timeline.tsx:138 (FIXED)
const displayGroups = timelineData?.days ? transformTimelineDays(timelineData.days) : []
// Result: timelineData.days contains real event data from backend
// Rendering: Displays events grouped by day
```

---

## 3. Type System Alignment

### 3.1 Current Frontend Types (INCORRECT)

**File**: `src/types/index.ts:28-31`

```typescript
export interface Timeline {
  entries: TimelineEntry[]  // ❌ Backend doesn't have this
  total: number              // ❌ Should be totalEvents
}

export interface TimelineEntry {
  id: string
  event: Event
  createdAt: string
  importance: number
}
```

**Problem**: No fields match the actual backend response.

### 3.2 Backend Types (SOURCE OF TRUTH)

**File**: `lifeai-backend/src/main/java/com/lifeai/dto/TimelineResponse.java`

```java
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TimelineResponse {
    private Long userId;
    private LocalDate fromDate;
    private LocalDate toDate;
    private Integer totalEvents;
    private List<TimelineDayResponse> days;  // ← KEY FIELD
}
```

**File**: `lifeai-backend/src/main/java/com/lifeai/dto/TimelineDayResponse.java`

```java
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TimelineDayResponse {
    private LocalDate date;
    private List<TimelineEventResponse> events;
    private Integer eventCount;
}
```

**File**: `lifeai-backend/src/main/java/com/lifeai/dto/TimelineEventResponse.java`

```java
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TimelineEventResponse {
    private Long id;
    private Long userId;
    private String type;
    private String description;
    private LocalDateTime timestamp;
    private Map<String, Object> metadata;
}
```

### 3.3 Corrected Frontend Types (TARGET)

**File**: `src/types/index.ts` (to be updated)

```typescript
// Matches backend TimelineEventResponse exactly
export interface TimelineEvent {
  id: number
  userId: number
  type: string
  description: string
  timestamp: string  // ISO-8601 from backend LocalDateTime
  metadata?: Record<string, unknown>
}

// Matches backend TimelineDayResponse exactly
export interface TimelineDay {
  date: string  // ISO-8601 date from backend LocalDate
  events: TimelineEvent[]
  eventCount: number
}

// Matches backend TimelineResponse exactly
export interface Timeline {
  userId: number
  fromDate: string  // ISO-8601 date
  toDate: string    // ISO-8601 date
  totalEvents: number
  days: TimelineDay[]
}

// Kept for backward compatibility if needed elsewhere
export interface TimelineEntry {
  id: string
  event: Event
  createdAt: string
  importance: number
}
```

**Migration Strategy**:
- Add new `TimelineEvent` and `TimelineDay` interfaces
- Update `Timeline` interface to match backend exactly
- Keep `TimelineEntry` if used elsewhere; mark as deprecated with comment
- Update all imports in components/hooks that reference `Timeline`

---

## 4. Frontend Parsing Logic

### 4.1 Current Component Implementation (BROKEN)

**File**: `src/components/Timeline.tsx:15-38`

```typescript
const groupEntriesByDay = (entries: TimelineEntry[]): TimelineGroup[] => {
  // Expects flat array of TimelineEntry objects with createdAt
  // Groups by date, then sorts by date descending
  // This function assumes entries already exist in state
}
```

**Issues**:
- Function expects flat `TimelineEntry[]` array
- Backend returns pre-grouped `TimelineDay[]` array
- Redundant client-side grouping when backend already groups
- Function accesses `entry.createdAt` but backend uses `timestamp`

### 4.2 Target Implementation (FIXED)

The backend already groups by day. The frontend should:

1. **Accept pre-grouped data** from backend (`days` array)
2. **Transform once** to internal rendering structure (if needed)
3. **Iterate over days** and render each day's events
4. **No re-grouping or re-sorting** (backend already handled)

**Pseudocode**:

```typescript
// Transform backend TimelineDays to rendering structure
const transformTimelineDays = (days: TimelineDay[]): TimelineGroup[] => {
  // Validate input
  if (!Array.isArray(days)) {
    console.warn('Invalid days array, returning empty')
    return []
  }

  // Map each backend day to rendering day
  return days
    .filter(day => day && day.events && Array.isArray(day.events))  // Defensive
    .map(day => ({
      date: day.date,  // ISO-8601 string
      entries: day.events.map(event => ({
        // Transform backend TimelineEvent to TimelineEntry shape
        id: String(event.id),
        event: {
          type: event.type,
          description: event.description,
          timestamp: event.timestamp,
          metadata: event.metadata,
        },
        createdAt: event.timestamp,  // Use timestamp as createdAt for consistency
        importance: extractImportance(event),  // Derive from metadata or default
      })),
    }))
}

// Helper: Extract importance (not in backend, derive or default)
const extractImportance = (event: TimelineEvent): number => {
  // Backend TimelineEvent doesn't have importance field
  // Options:
  // 1. Default to 5 (neutral)
  // 2. Derive from metadata if present
  // 3. Calculate based on event type
  if (event.metadata?.importance !== undefined) {
    return Number(event.metadata.importance)
  }
  return 5  // Default neutral importance
}
```

**Rendering Integration**:

```typescript
// Timeline.tsx:136-138 (FIXED)
export const Timeline: React.FC<TimelineProps> = ({ limit = 50, offset = 0, userId }) => {
  const { fetch: fetchTimeline, status: timelineStatus, data: timelineData, error: timelineError } = useTimeline(limit, offset, userId)

  // OLD: const displayGroups = timelineData?.entries ? groupEntriesByDay(timelineData.entries) : []
  // NEW:
  const displayGroups = timelineData?.days 
    ? transformTimelineDays(timelineData.days) 
    : []

  // Rest of component uses displayGroups as before
  // No other changes needed to rendering logic
}
```

### 4.3 Defensive Handling Requirements

All parsing functions must handle:

1. **Null/undefined input**
   ```typescript
   if (!days || !Array.isArray(days)) return []
   ```

2. **Missing fields in array elements**
   ```typescript
   .filter(day => day && day.events && Array.isArray(day.events))
   ```

3. **Invalid data types**
   ```typescript
   const eventId = Number(event.id)  // Coerce to number
   if (isNaN(eventId)) console.warn('Invalid event ID:', event.id)
   ```

4. **Date parsing errors**
   ```typescript
   try {
     const parsedDate = new Date(event.timestamp)
     if (isNaN(parsedDate.getTime())) {
       console.warn('Invalid date:', event.timestamp)
       return 'Invalid date'
     }
     return parsedDate.toLocaleDateString('pl-PL')
   } catch (e) {
     console.warn('Date parsing failed:', e)
     return 'Invalid date'
   }
   ```

5. **Empty events array**
   ```typescript
   if (!day.events || day.events.length === 0) {
     // Skip day or show "No events this day"
   }
   ```

---

## 5. Hook Interface: useTimeline

### 5.1 Current Implementation

**File**: `src/hooks/index.ts:65-95`

```typescript
export const useTimeline = (limit?: number, offset?: number, userId?: number) => {
  const [status, setStatus] = useState<'idle' | 'pending' | 'success' | 'error'>('idle')
  const [data, setData] = useState<Timeline | null>(null)  // ← Timeline type
  const [error, setError] = useState<Error | null>(null)

  const fetch = useCallback(async () => {
    setStatus('pending')
    setData(null)
    setError(null)

    try {
      const response = await timelineApi.getTimeline(limit, offset, userId)
      if (response.data) {
        setData(response.data)
        setStatus('success')
        return response.data
      }
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to fetch timeline'
      setError(new Error(message))
      setStatus('error')
      throw err
    }
  }, [limit, offset, userId])

  useEffect(() => {
    fetch()
  }, [fetch])

  return { fetch, status, data, error }
}
```

### 5.2 Required Changes

**Type annotation change only**:
```typescript
const [data, setData] = useState<Timeline | null>(null)
// Timeline type is automatically corrected when src/types/index.ts is updated
```

**No behavioral changes needed** — the hook already:
- Calls the correct API endpoint (`/api/timeline`)
- Passes correct parameters (userId, limit, offset)
- Returns response data as-is from backend
- No transformation needed at hook level (done in component)

### 5.3 Hook Usage Pattern

```typescript
// Component usage (unchanged)
const { fetch: fetchTimeline, status: timelineStatus, data: timelineData, error: timelineError } = useTimeline(limit, offset, userId)

// Hook returns:
{
  fetch: async () => Promise<Timeline>,      // Refetch function
  status: 'idle' | 'pending' | 'success' | 'error',
  data: Timeline | null,                      // Now has correct shape
  error: Error | null,                        // Error message if fetch failed
}
```

---

## 6. Refresh Mechanism After Event Creation

### 6.1 Current Flow

1. **AudioRecorder.tsx** – User records and submits audio
2. **useTranscribe hook** – Calls `/api/speech-to-text/transcribe` endpoint
3. **Speech-to-text returns** – `{ transcription, event, recommendations }`
4. **Dashboard.tsx** – Displays event and recommendations
5. **Timeline** – **NOT automatically refreshed**

### 6.2 Problem

After creating an event via audio, the Timeline component doesn't automatically refresh. User must click "↻ Odśwież" button manually.

### 6.3 Solution: Manual Refresh Trigger

**Strategy**: Expose `fetch` function from `useTimeline` hook; call it after event creation.

**Implementation Location**: `src/components/Dashboard.tsx` (orchestrates AudioRecorder + Timeline)

**Pseudocode**:

```typescript
// Dashboard.tsx (orchestrator component)
export const Dashboard = () => {
  const { userId } = useUser()
  
  // Get timeline fetch function
  const timelineRef = useRef<{ fetch: () => Promise<Timeline> }>(null)
  
  // Handle successful event creation
  const handleEventCreated = async (event) => {
    console.log('Event created, refreshing timeline...')
    
    // Give backend time to persist (optional delay)
    await new Promise(resolve => setTimeout(resolve, 500))
    
    // Trigger timeline refresh
    if (timelineRef.current?.fetch) {
      try {
        await timelineRef.current.fetch()
      } catch (error) {
        console.warn('Failed to refresh timeline:', error)
      }
    }
  }

  return (
    <div>
      <AudioRecorder onEventCreated={handleEventCreated} />
      <Timeline ref={timelineRef} userId={userId} />
    </div>
  )
}
```

**Alternative (Simpler)**: Use event listener or context to signal timeline refresh

```typescript
// Create a simple refresh context
const TimelineRefreshContext = createContext<{ refresh: () => void }>({ refresh: () => {} })

// Provider wraps Dashboard
<TimelineRefreshContext.Provider value={{ refresh: () => timelineRef.current?.fetch() }}>
  <AudioRecorder />
  <Timeline ref={timelineRef} />
</TimelineRefreshContext.Provider>

// After event created, emit refresh
const { refresh } = useContext(TimelineRefreshContext)
refresh()
```

**For MVP**: Simple approach is acceptable — Timeline has manual "↻ Odśwież" button. Users click it to refresh. Automatic refresh can be added post-MVP.

---

## 7. API Client Integration

### 7.1 Current Implementation

**File**: `src/services/api.ts:46-66`

```typescript
export const timelineApi = {
  async getTimeline(limit?: number, offset?: number, userId?: number): Promise<ApiResponse<Timeline>> {
    try {
      const params = new URLSearchParams()
      if (userId !== undefined) params.append('userId', String(userId))
      if (limit !== undefined) params.append('limit', String(limit))
      if (offset !== undefined) params.append('offset', String(offset))
      
      const queryString = params.toString()
      const url = queryString ? `/api/timeline?${queryString}` : '/api/timeline'
      
      const response = await apiClient.get<ApiResponse<Timeline>>(url)
      return response.data
    } catch (error) {
      if (axios.isAxiosError(error)) {
        throw new Error(error.response?.data?.error || 'Failed to fetch timeline')
      }
      throw error
    }
  },
}
```

### 7.2 No Changes Needed

The API client is **already correct**:
- ✅ Calls `/api/timeline` endpoint (correct path with `/api` prefix)
- ✅ Accepts userId, limit, offset parameters
- ✅ Returns `Promise<ApiResponse<Timeline>>`
- ✅ Error handling is appropriate

**Note**: Backend adds `/api` prefix automatically via `server.servlet.context-path=/api` in `application.properties:16`

### 7.3 Type Annotation Update

When `Timeline` type is updated in `src/types/index.ts`, the API client automatically receives the corrected type:

```typescript
// Before update:
const response = await apiClient.get<ApiResponse<Timeline>>(url)  // Old Timeline shape
return response.data  // { entries: [...] }

// After update:
const response = await apiClient.get<ApiResponse<Timeline>>(url)  // New Timeline shape
return response.data  // { userId, fromDate, toDate, totalEvents, days: [...] }
```

---

## 8. Related Hooks

### 8.1 useUser Hook

**File**: `src/hooks/useUser.ts`

```typescript
export const useUser = () => {
  const [userId, setUserId] = useState<number | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    const initializeUser = () => {
      try {
        const storedUserId = localStorage.getItem(USER_ID_KEY)
        if (storedUserId) {
          setUserId(parseInt(storedUserId, 10))
        } else {
          setUserId(DEFAULT_USER_ID)  // Default: 1
          localStorage.setItem(USER_ID_KEY, String(DEFAULT_USER_ID))
        }
      } catch (error) {
        console.error('Failed to initialize user:', error)
        setUserId(DEFAULT_USER_ID)
      } finally {
        setIsLoading(false)
      }
    }

    initializeUser()
  }, [])

  return { userId, isLoading, updateUserId }
}
```

**Usage in Timeline**:
```typescript
const { userId } = useUser()
const { fetch: fetchTimeline, status, data, error } = useTimeline(limit, offset, userId)
// userId automatically filters timeline to current user
```

### 8.2 useTranscribe Hook

**File**: `src/hooks/index.ts:220-286`

Returns: `{ transcribe, status, transcribedText, event, recommendations, resetTranscription }`

**Integration with Timeline Refresh**:
- After `transcribe()` completes, `status` becomes `'success'`
- Component should trigger timeline refresh at this point
- Backend typically persists event within 100ms

---

## 9. Known Issues & Workarounds

### 9.1 Issue: No `importance` Field in Backend

**Problem**: Backend `TimelineEventResponse` does not include `importance` field, but frontend `Timeline.tsx` renders it (line 233).

**Status**: Not blocking. Frontend currently renders "Ważność: 5/10" (from `entry.importance`).

**Current Workaround** (in component):
```typescript
// Timeline.tsx:198
const healthStatus = getEventHealthStatus(entry.event.description)
// Uses only description, not importance
```

**Post-MVP Solution**: Add `importance` field to backend `TimelineEventResponse` or store in `metadata`.

### 9.2 Issue: Backend Date Format

**Problem**: Backend sends `LocalDate` (e.g., "2025-12-17") but `LocalDateTime` (e.g., "2025-12-17T10:00:00") for timestamp.

**Status**: Not blocking. Axios automatically deserializes to ISO-8601 strings.

**Frontend Handling**:
```typescript
const timestamp = event.timestamp  // "2025-12-17T10:00:00" (ISO-8601)
const dateStr = new Date(timestamp).toLocaleDateString('pl-PL')  // Works fine
```

### 9.3 Issue: Pagination Defaults Not Documented

**Problem**: Backend defaults limit=50, offset=0 if not provided. Frontend assumes same.

**Status**: Not blocking. Tested and working.

**Frontend Assumption**:
```typescript
const { fetch: fetchTimeline } = useTimeline(50, 0, userId)  // Matches backend defaults
```

### 9.4 Issue: Time Zone Handling

**Problem**: Backend timestamps may be in UTC; frontend renders in local time.

**Status**: Not blocking for MVP. All users currently in same region (Poland).

**Current Behavior**:
```typescript
const time = new Date(event.timestamp).toLocaleTimeString('pl-PL')
// Converts UTC to local browser time automatically
```

### 9.5 Issue: No Event Edit/Delete UI

**Problem**: Timeline displays events as read-only; no edit/delete functionality.

**Status**: Out of scope for MVP. Design decision: display-only for MVP.

**Post-MVP**: Add event detail view with edit/delete options.

---

## 10. Implementation Checklist

### Phase 1: Type Definitions
- [ ] Add `TimelineEvent` interface to `src/types/index.ts`
- [ ] Add `TimelineDay` interface to `src/types/index.ts`
- [ ] Update `Timeline` interface to match backend exactly
- [ ] Verify TypeScript compiles without errors

### Phase 2: Parsing Logic
- [ ] Create `transformTimelineDays()` function in `src/components/Timeline.tsx`
- [ ] Add defensive null/undefined checks
- [ ] Add defensive date parsing error handling
- [ ] Create `extractImportance()` helper function

### Phase 3: Component Update
- [ ] Update line 138: `timelineData?.entries` → `timelineData?.days`
- [ ] Replace `groupEntriesByDay(timelineData.entries)` with `transformTimelineDays(timelineData.days)`
- [ ] Verify rendering logic still works (should be unchanged)
- [ ] Verify empty state still shows "Brak zdarzeń"

### Phase 4: Hook Update
- [ ] Update hook type annotation: `useState<Timeline | null>(null)` (automatic after type update)
- [ ] Verify no behavioral changes needed
- [ ] Verify TypeScript compiles without errors

### Phase 5: Integration Testing
- [ ] Render Timeline component with mock data
- [ ] Verify events display correctly
- [ ] Verify events grouped by date
- [ ] Verify events sorted by time within day
- [ ] Test empty state (no events)
- [ ] Test error state (API error)
- [ ] Test loading state (pending)
- [ ] Click refresh button and verify data updates
- [ ] Open browser console and verify no errors

### Phase 6: Manual Testing
- [ ] Record audio event via AudioRecorder
- [ ] Check that event appears in Timeline
- [ ] Click "↻ Odśwież" button and verify still displays
- [ ] Check that events show correct descriptions and times
- [ ] Test with multiple events on same day
- [ ] Test with multiple days of events
- [ ] Test on small screen (responsive design)

---

## 11. Backend Endpoints Reference

### Timeline Endpoint

**Path**: `GET /api/timeline`

**Parameters**:
- `userId` (optional, long) – Filter by user ID; default: 1
- `from` (optional, ISO-8601 date) – Start date; default: 7 days ago
- `to` (optional, ISO-8601 date) – End date; default: tomorrow
- `limit` (optional, int) – Pagination limit; default: 50
- `offset` (optional, int) – Pagination offset; default: 0

**Response**:
```json
{
  "userId": 1,
  "fromDate": "2025-12-10",
  "toDate": "2025-12-18",
  "totalEvents": 5,
  "days": [
    {
      "date": "2025-12-17",
      "eventCount": 2,
      "events": [
        {
          "id": 123,
          "userId": 1,
          "type": "ACTIVITY",
          "description": "Evening walk",
          "timestamp": "2025-12-17T18:45:00",
          "metadata": null
        }
      ]
    }
  ]
}
```

**Controller**: `com.lifeai.controller.TimelineController.getTimeline()`  
**Service**: `com.lifeai.service.TimelineService.getTimeline()`

---

## 12. File Modifications Summary

| File | Change | Impact |
|------|--------|--------|
| `src/types/index.ts` | Add `TimelineEvent`, `TimelineDay` interfaces; update `Timeline` | Type safety |
| `src/components/Timeline.tsx` | Add `transformTimelineDays()` function; update line 138 | Fixes data parsing |
| `src/hooks/index.ts` | Type annotation auto-corrects | No code changes |
| `src/services/api.ts` | No changes needed | Already correct |

---

## 13. Success Criteria

1. **TypeScript Compilation**: `npm run build` passes with no errors
2. **Linting**: `npm run lint` passes with no warnings
3. **Component Rendering**: Timeline displays at least one event when `totalEvents > 0`
4. **Empty State**: Shows "Brak zdarzeń" when `totalEvents = 0`
5. **Error State**: Shows error message when API fails
6. **No Console Errors**: Browser console clean (no TypeScript or runtime errors)
7. **Refresh Works**: Clicking "↻ Odśwież" re-fetches and updates display
8. **Type Safety**: All Timeline-related types properly typed (no `any`)

---

## 14. References

### Documentation
- `docs/PROJECT_STATE.md` – Section 2.B (API routing), Section 3.3 (Timeline Controller), Section 10 (Known Issues)
- `docs/PRD_TIMELINE_FIX.md` – Requirements and acceptance criteria

### Backend Source Code
- `lifeai-backend/src/main/java/com/lifeai/dto/TimelineResponse.java`
- `lifeai-backend/src/main/java/com/lifeai/dto/TimelineDayResponse.java`
- `lifeai-backend/src/main/java/com/lifeai/dto/TimelineEventResponse.java`
- `lifeai-backend/src/main/java/com/lifeai/controller/TimelineController.java`

### Frontend Source Code
- `lifeai-frontend/src/types/index.ts` – Lines 28-31 (current Timeline type)
- `lifeai-frontend/src/components/Timeline.tsx` – Lines 1-252 (component)
- `lifeai-frontend/src/hooks/index.ts` – Lines 65-95 (useTimeline hook)
- `lifeai-frontend/src/services/api.ts` – Lines 46-66 (API client)
- `lifeai-frontend/src/hooks/useUser.ts` – Lines 1-43 (user hook)

---

## 15. Revision History

| Version | Date | Status | Notes |
|---------|------|--------|-------|
| 1.0 | 2025-12-17 | Ready | Initial Tech Spec based on PRD and source analysis |

---

## Sign-Off

This Tech Spec provides a complete, actionable plan for frontend implementation. All changes are non-breaking and preserve existing API contracts.

**Validated Against**:
- ✅ Backend source DTOs
- ✅ Frontend component code
- ✅ API client implementation
- ✅ Hook implementations
- ✅ Type system requirements
- ✅ PRD acceptance criteria
