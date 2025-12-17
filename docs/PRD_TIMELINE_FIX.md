# Product Requirements Document: Timeline Rendering Alignment (MVP Fix)

**Document Version**: 1.0  
**Date**: December 17, 2025  
**Status**: Open – Ready for Implementation  
**Priority**: High (MVP Blocker)

---

## 1. Overview

### User Need
As a user, I want my recorded events to appear correctly in the Timeline view, so that I can see my logged activities over time without missing or empty data.

### Current State
- **Backend**: Timeline API returns correctly structured data: `{ userId, fromDate, toDate, totalEvents, days: [...] }`
- **Frontend**: Timeline component expects field `entries` which does not exist in the backend response, causing the UI to render empty with "Brak zdarzeń" (No events)
- **Impact**: The MVP's core event visibility feature is non-functional; users cannot see their recorded events

### Feature Goal
Restore core MVP functionality by aligning the frontend Timeline rendering logic to correctly consume and display the backend's `days` array structure, without requiring any changes to the backend API or database schema.

---

## 2. Functional Requirements

### FR1: Parse Backend Timeline Response Structure
The frontend must correctly parse the backend's `TimelineResponse` structure:
```json
{
  "userId": <number>,
  "fromDate": "<ISO-8601 date>",
  "toDate": "<ISO-8601 date>",
  "totalEvents": <integer>,
  "days": [
    {
      "date": "<ISO-8601 date>",
      "events": [
        {
          "id": <number>,
          "userId": <number>,
          "type": "<event-type>",
          "description": "<event description>",
          "timestamp": "<ISO-8601 timestamp>",
          "metadata": { /* optional metadata */ }
        }
      ],
      "eventCount": <integer>
    }
  ]
}
```

### FR2: Display Events Grouped by Day
The Timeline component must:
- Display events grouped by calendar day
- Show date headers for each day
- Display all events within each day
- Show event type, description, and timestamp for each event

### FR3: Handle Empty States
The Timeline component must:
- Display "Brak zdarzeń" (No events) when `totalEvents` is 0
- Display "Brak zdarzeń" when the `days` array is empty or null
- Display appropriate error messaging when API calls fail

### FR4: Support Pagination Parameters
The Timeline hook (`useTimeline`) must:
- Accept `limit` parameter (items per page, default: 50)
- Accept `offset` parameter (pagination offset, default: 0)
- Accept `userId` parameter (filter by user, default: current user)
- Pass these parameters to the backend API call

### FR5: Maintain Existing Hook Interface
The `useTimeline` hook must continue to return:
```typescript
{
  fetch: () => Promise<Timeline>,
  status: 'idle' | 'pending' | 'success' | 'error',
  data: Timeline | null,
  error: Error | null
}
```

---

## 3. Non-Functional Requirements

### NFR1: Defensive Data Handling
- Gracefully handle missing or null `days` array
- Gracefully handle missing or malformed individual events
- Do not crash when event metadata is incomplete
- Do not crash when date parsing fails

### NFR2: Type Safety
- Update TypeScript interfaces to reflect the actual backend response structure
- Ensure the `Timeline` type in `src/types/index.ts` matches `TimelineResponse`
- Ensure the `TimelineDayResponse` type matches backend structure
- Ensure the `TimelineEventResponse` type matches backend structure

### NFR3: Backward Compatibility
- No changes to the backend API contract
- No changes to the backend response structure
- No changes to existing hook function signatures
- No changes to component prop interfaces (except where necessary to receive corrected data)

### NFR4: Performance
- Rendering should handle 100+ events per day without lag
- No unnecessary re-renders when data updates
- Efficient grouping algorithm (linear time, no repeated sorting)

---

## 4. Acceptance Criteria

### AC1: Timeline Data Structure
- [ ] Backend API endpoint `/api/timeline` returns `{ userId, fromDate, toDate, totalEvents, days: [...] }`
- [ ] Frontend types correctly model this structure
- [ ] `useTimeline` hook receives and stores the correct structure

### AC2: Events Display
- [ ] Timeline component displays at least one event when `totalEvents > 0`
- [ ] Events are grouped visually by date (date header, then events)
- [ ] Each event shows: type, description, and timestamp
- [ ] Date format is user-friendly (e.g., "December 17, 2025")

### AC3: Empty State
- [ ] When `totalEvents = 0`, display "Brak zdarzeń"
- [ ] When `days` array is empty, display "Brak zdarzeń"
- [ ] When API error occurs, display error message from hook

### AC4: Manual Refresh
- [ ] Clicking "↻ Odśwież" button re-fetches timeline data
- [ ] Loading spinner appears during fetch
- [ ] Data updates on screen after successful fetch

### AC5: Defensive Handling
- [ ] No console errors when `days` is null or undefined
- [ ] No console errors when individual events are malformed
- [ ] No console errors when date parsing fails (graceful fallback to "Invalid date")

### AC6: Type Safety
- [ ] No TypeScript errors in Timeline component
- [ ] No TypeScript errors in useTimeline hook
- [ ] No `any` types used for timeline data structures
- [ ] Frontend types are aligned with PROJECT_STATE.md documented backend schema

---

## 5. Technical Design Notes

### 5.1 Current Issue: Schema Mismatch

**What the backend returns** (from `TimelineResponse.java`):
```java
private List<TimelineDayResponse> days;  // ← Key field
```

**What the frontend expects** (from `src/types/index.ts`, line 29):
```typescript
entries: TimelineEntry[]  // ← Different field name
```

**Current Code** (Timeline.tsx, line 138):
```typescript
const displayGroups = timelineData?.entries ? groupEntriesByDay(timelineData.entries) : []
```

**Result**: `timelineData.entries` is always undefined because the backend sends `days`, not `entries`. The timeline renders empty.

### 5.2 Solution Approach

1. **Update Frontend Types** (`src/types/index.ts`):
   - Rename `Timeline` interface field from `entries` to `days`
   - Add/update `TimelineDayResponse` interface to match backend DTO
   - Add/update `TimelineEventResponse` interface to match backend DTO
   - Ensure all field names align with backend @JsonProperty annotations

2. **Update useTimeline Hook** (`src/hooks/index.ts`):
   - No API call changes needed (already using correct endpoint)
   - Ensure type annotations use updated `Timeline` type
   - No behavioral changes needed

3. **Update Timeline Component** (`src/components/Timeline.tsx`):
   - Replace `timelineData?.entries` with `timelineData?.days`
   - Update `groupEntriesByDay()` function to accept `days` array instead of `entries`
   - Ensure grouping logic iterates over `days[].events` (already grouped by day by backend)
   - Add defensive checks for null/undefined `days`

4. **Add Type Guards and Error Handling**:
   - Validate that `days` is an array before rendering
   - Validate that each day's `events` array exists before rendering
   - Validate that each event has required fields (id, type, description, timestamp)
   - Gracefully skip or mark invalid events

### 5.3 Backend Response Example

Real response from backend:
```json
{
  "userId": 1,
  "fromDate": "2025-12-10",
  "toDate": "2025-12-17",
  "totalEvents": 3,
  "days": [
    {
      "date": "2025-12-17",
      "eventCount": 2,
      "events": [
        {
          "id": 101,
          "userId": 1,
          "type": "FOOD",
          "description": "Lunch at home",
          "timestamp": "2025-12-17T12:30:00"
        },
        {
          "id": 102,
          "userId": 1,
          "type": "ACTIVITY",
          "description": "Evening walk",
          "timestamp": "2025-12-17T18:45:00"
        }
      ]
    },
    {
      "date": "2025-12-16",
      "eventCount": 1,
      "events": [
        {
          "id": 100,
          "userId": 1,
          "type": "MEDICATION",
          "description": "Took aspirin",
          "timestamp": "2025-12-16T09:00:00"
        }
      ]
    }
  ]
}
```

### 5.4 Grouping Strategy

**Backend already groups by day** – the `days` array is pre-sorted and pre-grouped by the backend. The frontend should:
- **NOT re-group** the data
- **NOT re-sort** the data
- Simply iterate over `days` array and render each day with its events
- This is more efficient than the current approach of flat event array + client-side grouping

### 5.5 Known Limitations (By Design)

- Pagination is basic (limit/offset); no infinite scroll
- Date range filtering is determined by backend
- No client-side filtering by event type (can be added later)
- No event detail view (can be added later)
- Event metadata is stored but not displayed (can be added later)

---

## 6. Constraints

### Must Not Change
- **Backend API**: No new endpoints, no changes to `/api/timeline` response
- **Backend DTO**: No changes to `TimelineResponse`, `TimelineDayResponse`, `TimelineEventResponse`
- **Database Schema**: No new tables, no schema changes
- **Hook Signature**: `useTimeline(limit?, offset?, userId?)` remains the same
- **Component Props**: No new props added to Timeline component (only internal data structure changes)

### Out of Scope
- User authentication/login flow
- Calendar view (planned separately)
- Event type filtering by UI
- AI-powered recommendations integration
- Notification system
- Event editing/deletion
- Event detail view expansion
- Infinite scroll pagination

---

## 7. Success Metrics

1. **Functional**: Timeline displays at least one event when user has recorded events
2. **Reliability**: No console errors when rendering timeline with various data states
3. **Type Safety**: TypeScript build passes with no errors
4. **Testing**: Manual testing confirms empty states and multi-event days display correctly

---

## 8. Dependencies & References

**Related Documentation**:
- `docs/PROJECT_STATE.md` – Section 2.B (API routing), Section 3.3 (Timeline Controller), Section 10 (Known Issues)

**Backend Source Code**:
- `src/main/java/com/lifeai/dto/TimelineResponse.java`
- `src/main/java/com/lifeai/dto/TimelineDayResponse.java`
- `src/main/java/com/lifeai/dto/TimelineEventResponse.java`
- `src/main/java/com/lifeai/controller/TimelineController.java`

**Frontend Source Code**:
- `src/types/index.ts` – Line 28-31 (Timeline interface)
- `src/hooks/index.ts` – Line 65-95 (useTimeline hook)
- `src/components/Timeline.tsx` – Line 136-200 (Timeline component)
- `src/api/timelineApi.ts` – API client for timeline endpoint

---

## 9. Revision History

| Version | Date | Author | Status | Notes |
|---------|------|--------|--------|-------|
| 1.0 | 2025-12-17 | AI | Open | Initial PRD based on PROJECT_STATE.md validation findings |

---

## 10. Sign-Off

This PRD reflects the validated current state of the LifeAI project per `docs/PROJECT_STATE.md`. It is ready for Technical Specification and Implementation Plan phases.

**Reviewed Against**:
- ✅ Backend source code and DTOs
- ✅ Frontend components and hooks
- ✅ TypeScript type definitions
- ✅ API routing configuration
- ✅ PROJECT_STATE.md documented schema mismatch
