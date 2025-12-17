import axios from 'axios'
import { Event, Timeline, Recommendation, ApiResponse, TranscriptionWithEventResponse } from '../types'

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

export const eventApi = {
  async sendEvent(event: Event): Promise<ApiResponse<Event>> {
    try {
      const response = await apiClient.post<ApiResponse<Event>>('/api/events', event)
      return response.data
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

export const recommendationsApi = {
  async getRecommendations(category?: string, userId?: number): Promise<ApiResponse<Recommendation[]>> {
    try {
      const params = new URLSearchParams()
      if (userId !== undefined) params.append('userId', String(userId))
      if (category) params.append('category', category)
      
      const queryString = params.toString()
      const url = queryString ? `/api/recommendations?${queryString}` : '/api/recommendations'
      
      const response = await apiClient.get<ApiResponse<Recommendation[]>>(url)
      return response.data
    } catch (error) {
      if (axios.isAxiosError(error)) {
        throw new Error(error.response?.data?.error || 'Failed to fetch recommendations')
      }
      throw error
    }
  },

  async markRecommendationDone(id: string): Promise<ApiResponse<Recommendation>> {
    try {
      const response = await apiClient.patch<ApiResponse<Recommendation>>(
        `/api/recommendations/${id}/done`
      )
      return response.data
    } catch (error) {
      if (axios.isAxiosError(error)) {
        throw new Error(error.response?.data?.error || 'Failed to update recommendation')
      }
      throw error
    }
  },

  async markRecommendationPlanned(id: string): Promise<ApiResponse<Recommendation>> {
    try {
      const response = await apiClient.patch<ApiResponse<Recommendation>>(
        `/api/recommendations/${id}/planned`
      )
      return response.data
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
