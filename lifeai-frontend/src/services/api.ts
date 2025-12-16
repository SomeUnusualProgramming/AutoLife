import axios from 'axios'
import { Event, Timeline, Recommendation, ApiResponse } from '../types'

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
  timeout: 10000,
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
  async getTimeline(limit?: number, offset?: number): Promise<ApiResponse<Timeline>> {
    try {
      const params = new URLSearchParams()
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
  async getRecommendations(category?: string): Promise<ApiResponse<Recommendation[]>> {
    try {
      const params = new URLSearchParams()
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
