export interface ApiResponse<T = unknown> {
  success: boolean
  data?: T
  error?: string
}

export interface User {
  id: number
  username: string
  email: string
}

export interface Event {
  id?: string
  userId?: number
  type: string
  description: string
  timestamp?: string
  metadata?: Record<string, unknown>
}

export interface TimelineEntry {
  id: string
  event: Event
  createdAt: string
  importance: number
}

export interface Timeline {
  entries: TimelineEntry[]
  total: number
}

export interface Recommendation {
  id: string
  category: string
  suggestion: string
  priority: 'low' | 'medium' | 'high'
  status: 'PLANNED' | 'DONE'
  actionUrl?: string
  aiGenerated?: boolean
  createdAt?: string
}

export interface TranscriptionResult {
  text: string
  sourceFile?: string
  confidence?: number
  language?: string
  processingTimeMs?: number
}

export interface TranscriptionWithEventResponse {
  transcription: TranscriptionResult
  event?: {
    id: number
    userId: number
    type: string
    description: string
    timestamp: string
    metadata?: Record<string, unknown>
  }
  recommendations?: Array<{
    id: number
    userId: number
    eventId: number
    text: string
    type: string
    priority: 'HIGH' | 'MEDIUM' | 'LOW'
    status: string
    isApplied?: boolean
    createdAt?: string
  }>
}

export interface AiAgentRequest {
  inputType: 'TEXT' | 'AUDIO'
  textInput?: string
  transcribedText?: string
  userId: number
  historicalContext?: Record<string, unknown>
}

export interface AiAgentResponse {
  status: 'COMPLETE' | 'PENDING_CLARIFICATION' | 'ERROR' | 'INCOMPLETE_AUTO_SAVED'
  action: 'CREATE_EVENT' | 'ASK_QUESTION' | 'IGNORE'
  event?: Event
  pendingEvent?: {
    id: number
    sessionId: string
    type: string
    rawInput: string
    metadata?: Record<string, unknown>
    confidence: number
    status: string
    clarificationRound: number
  }
  clarificationQuestion?: string
  clarificationRound?: number
  sessionId?: string
  aiResponse: string
  confidence?: number
  warningFlags?: string[]
}

export interface ClarificationMessage {
  role: 'user' | 'ai'
  content: string
  timestamp?: string
}

export interface ClarificationSession {
  sessionId: string
  clarificationRound: number
  question: string
  conversationHistory: ClarificationMessage[]
  pendingEvent?: {
    id: number
    type: string
    confidence: number
    metadata?: Record<string, unknown>
  }
  status: 'ACTIVE' | 'COMPLETED' | 'ABANDONED'
}
