import { useState, useCallback } from 'react'
import { Event, Timeline, Recommendation } from '../types'
import { eventApi, timelineApi, recommendationsApi } from '../services/api'

export const useAsync = <T,>(
  asyncFunction: () => Promise<T>,
  immediate = true
) => {
  const [status, setStatus] = useState<'idle' | 'pending' | 'success' | 'error'>('idle')
  const [data, setData] = useState<T | null>(null)
  const [error, setError] = useState<Error | null>(null)

  const execute = useCallback(async () => {
    setStatus('pending')
    setData(null)
    setError(null)

    try {
      const response = await asyncFunction()
      setData(response)
      setStatus('success')
      return response
    } catch (err) {
      setError(err instanceof Error ? err : new Error(String(err)))
      setStatus('error')
    }
  }, [asyncFunction])

  if (immediate) {
    execute()
  }

  return { execute, status, data, error }
}

export const useSendEvent = () => {
  const [status, setStatus] = useState<'idle' | 'pending' | 'success' | 'error'>('idle')
  const [data, setData] = useState<Event | null>(null)
  const [error, setError] = useState<Error | null>(null)

  const send = useCallback(async (event: Event) => {
    setStatus('pending')
    setData(null)
    setError(null)

    try {
      const response = await eventApi.sendEvent(event)
      if (response.data) {
        setData(response.data)
        setStatus('success')
        return response.data
      }
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to send event'
      setError(new Error(message))
      setStatus('error')
      throw err
    }
  }, [])

  return { send, status, data, error }
}

export const useTimeline = (limit?: number, offset?: number) => {
  const [status, setStatus] = useState<'idle' | 'pending' | 'success' | 'error'>('idle')
  const [data, setData] = useState<Timeline | null>(null)
  const [error, setError] = useState<Error | null>(null)

  const fetch = useCallback(async () => {
    setStatus('pending')
    setData(null)
    setError(null)

    try {
      const response = await timelineApi.getTimeline(limit, offset)
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
  }, [limit, offset])

  fetch()

  return { fetch, status, data, error }
}

export const useRecommendations = (category?: string) => {
  const [status, setStatus] = useState<'idle' | 'pending' | 'success' | 'error'>('idle')
  const [data, setData] = useState<Recommendation[] | null>(null)
  const [error, setError] = useState<Error | null>(null)

  const fetch = useCallback(async () => {
    setStatus('pending')
    setData(null)
    setError(null)

    try {
      const response = await recommendationsApi.getRecommendations(category)
      if (response.data) {
        setData(response.data)
        setStatus('success')
        return response.data
      }
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to fetch recommendations'
      setError(new Error(message))
      setStatus('error')
      throw err
    }
  }, [category])

  fetch()

  return { fetch, status, data, error }
}
