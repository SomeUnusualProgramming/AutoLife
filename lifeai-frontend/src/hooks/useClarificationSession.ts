import { useState, useCallback } from 'react'
import { AiAgentResponse } from '../types'
import { api } from '../services/api'

export const useClarificationSession = (initialSessionId?: string) => {
  const [sessionId, setSessionId] = useState<string | undefined>(initialSessionId)
  const [conversation, setConversation] = useState<Array<{ role: string; content: string }>>([])
  const [pendingEvent, setPendingEvent] = useState<any>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [clarificationRound, setClarificationRound] = useState(0)

  const submitResponse = useCallback(async (userResponse: string) => {
    if (!sessionId) {
      setError('No active session')
      return null
    }

    setLoading(true)
    setError(null)

    try {
      const response = await api.post<AiAgentResponse>(
        `/api/clarification/${sessionId}/respond`,
        { userResponse, sessionId }
      )

      if (response.data) {
        setConversation(prev => [
          ...prev,
          { role: 'user', content: userResponse },
          { role: 'ai', content: response.data.aiResponse }
        ])

        setClarificationRound(response.data.clarificationRound || 0)
        setPendingEvent(response.data.pendingEvent)

        return response.data
      }
    } catch (err: any) {
      const errorMsg = err.response?.data?.error || 'Failed to submit response'
      setError(errorMsg)
      console.error('Error submitting clarification response:', err)
    } finally {
      setLoading(false)
    }

    return null
  }, [sessionId])

  const resumeSession = useCallback(async (id: string) => {
    setLoading(true)
    setError(null)

    try {
      const response = await api.get<any>(`/api/clarification/${id}`)
      
      if (response.data) {
        setSessionId(id)
        const history = response.data.qa_history || []
        const newConversation = history.flatMap((qa: any) => [
          { role: 'ai', content: qa.question },
          { role: 'user', content: qa.response }
        ])
        setConversation(newConversation)
        setClarificationRound(response.data.total_rounds || 0)
      }
    } catch (err: any) {
      const errorMsg = err.response?.data?.error || 'Failed to resume session'
      setError(errorMsg)
      console.error('Error resuming session:', err)
    } finally {
      setLoading(false)
    }
  }, [])

  const cancelSession = useCallback(() => {
    setSessionId(undefined)
    setConversation([])
    setPendingEvent(null)
    setError(null)
    setClarificationRound(0)
    
    if (sessionId) {
      localStorage.removeItem(`clarification_${sessionId}`)
    }
  }, [sessionId])

  return {
    sessionId,
    conversation,
    pendingEvent,
    loading,
    error,
    clarificationRound,
    submitResponse,
    resumeSession,
    cancelSession,
    setSessionId
  }
}
