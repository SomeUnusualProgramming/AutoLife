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
  actionUrl?: string
}
