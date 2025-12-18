import axios from 'axios'
import { Event, Timeline, Recommendation, ApiResponse, TranscriptionWithEventResponse, TimelineEntry, AiAgentRequest, AiAgentResponse } from '../types'

const getApiBaseUrl = () => {
  if (import.meta.env.VITE_API_URL) {
    return import.meta.env.VITE_API_URL
  }
  if (typeof window !== 'undefined') {
    return `${window.location.protocol}//${window.location.hostname}:8080`
  }
  return 'http://localhost:8080'
}

export const apiClient = axios.create({
  baseURL: getApiBaseUrl(),
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
})

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      console.error('Unauthorized')
    }
    return Promise.reject(error)
  }
)

const transformTimelineResponse = (backendResponse: any): Timeline => {
  if (backendResponse.entries) {
    return backendResponse as Timeline
  }

  if (backendResponse.days && Array.isArray(backendResponse.days)) {
    const entries: TimelineEntry[] = []
    let entryId = 1

    backendResponse.days.forEach((day: any) => {
      if (day.events && Array.isArray(day.events)) {
        day.events.forEach((event: any) => {
          entries.push({
            id: `timeline-${entryId++}`,
            event: {
              id: event.id?.toString(),
              type: event.type,
              description: event.description,
              timestamp: event.timestamp,
              metadata: event.metadata || {},
            },
            createdAt: event.timestamp,
            importance: 5,
          })
        })
      }
    })

    return {
      entries,
      total: backendResponse.totalEvents || entries.length,
    }
  }

  return { entries: [], total: 0 }
}

const transformRecommendationResponse = (rec: any): Recommendation => {
  return {
    id: String(rec.id),
    category: rec.category || rec.type || 'HEALTH',
    suggestion: rec.suggestion || rec.text || '',
    priority: (rec.priority?.toLowerCase() || 'medium') as 'low' | 'medium' | 'high',
    status: (rec.status === 'DONE' || rec.isApplied ? 'DONE' : 'PLANNED') as 'PLANNED' | 'DONE',
    actionUrl: rec.actionUrl,
    aiGenerated: rec.aiGenerated || false,
    createdAt: rec.createdAt,
  }
}

export const eventApi = {
  async sendEvent(event: Event): Promise<ApiResponse<Event>> {
    try {
      const response = await apiClient.post<Event>('/api/events', event)
      return {
        success: true,
        data: response.data
      }
    } catch (error) {
      if (axios.isAxiosError(error)) {
        throw new Error(error.response?.data?.error || 'Failed to send event')
      }
      throw error
    }
  },
}

export const timelineApi = {
  async getTimeline(limit?: number, offset?: number, userId?: number): Promise<ApiResponse<Timeline>> {
    try {
      const params = new URLSearchParams()
      if (userId !== undefined) params.append('userId', String(userId))
      if (limit !== undefined) params.append('limit', String(limit))
      if (offset !== undefined) params.append('offset', String(offset))
      
      const queryString = params.toString()
      const url = queryString ? `/api/timeline?${queryString}` : '/api/timeline'
      
      const response = await apiClient.get<ApiResponse<any>>(url)
      const transformedData = transformTimelineResponse(response.data.data || response.data)
      
      return {
        success: response.data.success,
        data: transformedData,
      }
    } catch (error) {
      if (axios.isAxiosError(error)) {
        throw new Error(error.response?.data?.error || 'Failed to fetch timeline')
      }
      throw error
    }
  },
}

export const recommendationsApi = {
  async getRecommendations(category?: string, userId?: number): Promise<ApiResponse<Recommendation[]>> {
    try {
      let url = '/api/recommendations'
      
      if (userId !== undefined) {
        url = `/api/recommendations/users/${userId}`
      }
      
      const params = new URLSearchParams()
      if (category) params.append('category', category)
      
      const queryString = params.toString()
      if (queryString) {
        url += `?${queryString}`
      }
      
      const response = await apiClient.get<ApiResponse<any[]>>(url)
      const recs = Array.isArray(response.data) ? response.data : response.data.data || []
      const transformedRecs = recs.map(transformRecommendationResponse)
      
      return {
        success: response.data.success !== false,
        data: transformedRecs,
      }
    } catch (error) {
      if (axios.isAxiosError(error)) {
        throw new Error(error.response?.data?.error || 'Failed to fetch recommendations')
      }
      throw error
    }
  },

  async markRecommendationDone(id: string): Promise<ApiResponse<Recommendation>> {
    try {
      const response = await apiClient.patch<ApiResponse<any>>(
        `/api/recommendations/${id}/done`
      )
      const transformedRec = transformRecommendationResponse(response.data.data || response.data)
      return {
        success: response.data.success,
        data: transformedRec,
      }
    } catch (error) {
      if (axios.isAxiosError(error)) {
        throw new Error(error.response?.data?.error || 'Failed to update recommendation')
      }
      throw error
    }
  },

  async markRecommendationPlanned(id: string): Promise<ApiResponse<Recommendation>> {
    try {
      const response = await apiClient.patch<ApiResponse<any>>(
        `/api/recommendations/${id}/planned`
      )
      const transformedRec = transformRecommendationResponse(response.data.data || response.data)
      return {
        success: response.data.success,
        data: transformedRec,
      }
    } catch (error) {
      if (axios.isAxiosError(error)) {
        throw new Error(error.response?.data?.error || 'Failed to update recommendation')
      }
      throw error
    }
  },
}

export const speechToTextApi = {
  async transcribeAudio(audioFile: File, language?: string, userId?: number): Promise<TranscriptionWithEventResponse> {
    try {
      const formData = new FormData()
      formData.append('file', audioFile)
      if (language) formData.append('language', language)
      if (userId) formData.append('userId', String(userId))

      const response = await apiClient.post<TranscriptionWithEventResponse>(
        '/api/speech-to-text/transcribe',
        formData,
        {
          headers: {
            'Content-Type': 'multipart/form-data',
          },
          timeout: 60000,
        }
      )
      
      if (response.data.transcription) {
        return response.data
      }
      
      throw new Error('Invalid transcription response format')
    } catch (error) {
      if (axios.isAxiosError(error)) {
        const errorMessage = 
          error.response?.data?.error || 
          error.response?.data?.detail || 
          error.response?.statusText || 
          'Failed to transcribe audio'
        throw new Error(errorMessage)
      }
      throw error
    }
  },

  async transcribeAudioBytes(audioBytes: Blob, mimeType: string, language?: string, userId?: number): Promise<TranscriptionWithEventResponse> {
    try {
      const formData = new FormData()
      formData.append('audioBytes', audioBytes)
      formData.append('mimeType', mimeType)
      if (language) formData.append('language', language)
      if (userId) formData.append('userId', String(userId))

      const response = await apiClient.post<TranscriptionWithEventResponse>(
        '/api/speech-to-text/transcribe-bytes',
        formData,
        {
          headers: {
            'Content-Type': 'multipart/form-data',
          },
          timeout: 60000,
        }
      )
      
      if (response.data.transcription) {
        return response.data
      }
      
      throw new Error('Invalid transcription response format')
    } catch (error) {
      if (axios.isAxiosError(error)) {
        const errorMessage = 
          error.response?.data?.error || 
          error.response?.data?.detail || 
          error.response?.statusText || 
          'Failed to transcribe audio'
        throw new Error(errorMessage)
      }
      throw error
    }
  },
}

export const aiAgentApi = {
  async processEventInput(request: AiAgentRequest): Promise<AiAgentResponse> {
    try {
      const response = await apiClient.post<AiAgentResponse>(
        '/api/speech-to-text/process',
        request
      )
      return response.data
    } catch (error) {
      if (axios.isAxiosError(error)) {
        throw new Error(error.response?.data?.aiResponse || 'Failed to process event')
      }
      throw error
    }
  },

  async respondToClarification(sessionId: string, userResponse: string): Promise<AiAgentResponse> {
    try {
      const response = await apiClient.post<AiAgentResponse>(
        `/api/clarification/${sessionId}/respond`,
        { sessionId, userResponse }
      )
      return response.data
    } catch (error) {
      if (axios.isAxiosError(error)) {
        throw new Error(error.response?.data?.aiResponse || 'Failed to submit clarification')
      }
      throw error
    }
  },

  async getSessionContext(sessionId: string): Promise<any> {
    try {
      const response = await apiClient.get<any>(
        `/api/clarification/${sessionId}`
      )
      return response.data
    } catch (error) {
      if (axios.isAxiosError(error)) {
        throw new Error(error.response?.data?.error || 'Failed to fetch session context')
      }
      throw error
    }
  },
}

export const api = apiClient
