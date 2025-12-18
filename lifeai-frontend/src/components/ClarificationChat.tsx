import { useState, useEffect, useRef } from 'react'
import { AiAgentResponse, ClarificationMessage } from '../types'
import { api } from '../services/api'

interface ClarificationChatProps {
  sessionId?: string
  onEventComplete?: (event: any) => void
  onCancel?: () => void
}

export const ClarificationChat = ({
  sessionId,
  onEventComplete,
  onCancel
}: ClarificationChatProps) => {
  const [conversation, setConversation] = useState<ClarificationMessage[]>([])
  const [userInput, setUserInput] = useState('')
  const [loading, setLoading] = useState(false)
  const [currentSessionId, setCurrentSessionId] = useState<string | undefined>(sessionId)
  const [currentQuestion, setCurrentQuestion] = useState<string>('')
  const [clarificationRound, setClarificationRound] = useState(0)
  const scrollRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    // Load from localStorage on mount
    const savedSession = localStorage.getItem(`clarification_${sessionId || 'active'}`)
    if (savedSession) {
      const parsed = JSON.parse(savedSession)
      setConversation(parsed.history || [])
      setCurrentQuestion(parsed.question || '')
      setClarificationRound(parsed.round || 0)
      setCurrentSessionId(parsed.sessionId)
    }
  }, [sessionId])

  useEffect(() => {
    // Auto-scroll to bottom
    if (scrollRef.current) {
      scrollRef.current.scrollIntoView({ behavior: 'smooth' })
    }
  }, [conversation])

  useEffect(() => {
    // Save to localStorage whenever conversation changes
    if (currentSessionId) {
      const sessionData = {
        sessionId: currentSessionId,
        history: conversation,
        question: currentQuestion,
        round: clarificationRound
      }
      localStorage.setItem(`clarification_${currentSessionId}`, JSON.stringify(sessionData))
    }
  }, [conversation, currentSessionId, currentQuestion, clarificationRound])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    
    if (!userInput.trim() || !currentSessionId) return

    setLoading(true)
    
    try {
      const newMessage: ClarificationMessage = {
        role: 'user',
        content: userInput,
        timestamp: new Date().toISOString()
      }
      
      setConversation(prev => [...prev, newMessage])
      setUserInput('')

      const response = await api.post<AiAgentResponse>(
        `/api/clarification/${currentSessionId}/respond`,
        { userResponse: userInput, sessionId: currentSessionId }
      )

      if (response.data) {
        const aiMessage: ClarificationMessage = {
          role: 'ai',
          content: response.data.aiResponse,
          timestamp: new Date().toISOString()
        }
        
        setConversation(prev => [...prev, aiMessage])
        setClarificationRound(response.data.clarificationRound || clarificationRound + 1)

        if (response.data.status === 'COMPLETE') {
          // Event completed successfully
          if (response.data.event && onEventComplete) {
            onEventComplete(response.data.event)
          }
          // Clean up localStorage
          localStorage.removeItem(`clarification_${currentSessionId}`)
          onCancel?.()
        } else if (response.data.status === 'INCOMPLETE_AUTO_SAVED') {
          // Auto-saved with warning
          if (response.data.event && onEventComplete) {
            onEventComplete(response.data.event)
          }
          localStorage.removeItem(`clarification_${currentSessionId}`)
          onCancel?.()
        } else if (response.data.clarificationQuestion) {
          setCurrentQuestion(response.data.clarificationQuestion)
        }
      }
    } catch (error) {
      console.error('Error submitting clarification:', error)
      const errorMessage: ClarificationMessage = {
        role: 'ai',
        content: 'Przepraszam, wystąpił błąd. Spróbuj ponownie.',
        timestamp: new Date().toISOString()
      }
      setConversation(prev => [...prev, errorMessage])
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="w-full max-w-2xl mx-auto p-4 bg-white rounded-lg shadow-lg">
      <div className="flex justify-between items-center mb-4">
        <h2 className="text-lg font-semibold text-gray-800">AI Clarification</h2>
        {onCancel && (
          <button
            onClick={onCancel}
            className="text-gray-500 hover:text-gray-700"
          >
            ✕
          </button>
        )}
      </div>

      <div className="bg-gray-50 rounded-lg p-4 mb-4 h-96 overflow-y-auto">
        {conversation.length === 0 && currentQuestion ? (
          <div className="text-left">
            <div className="bg-blue-100 text-blue-800 p-3 rounded-lg inline-block max-w-xs">
              {currentQuestion}
            </div>
          </div>
        ) : (
          conversation.map((msg, idx) => (
            <div
              key={idx}
              className={`mb-3 flex ${msg.role === 'user' ? 'justify-end' : 'justify-start'}`}
            >
              <div
                className={`p-3 rounded-lg max-w-xs ${
                  msg.role === 'user'
                    ? 'bg-emerald-100 text-emerald-800'
                    : 'bg-blue-100 text-blue-800'
                }`}
              >
                <p className="text-sm">{msg.content}</p>
              </div>
            </div>
          ))
        )}
        {loading && (
          <div className="flex justify-start">
            <div className="bg-gray-200 text-gray-800 p-3 rounded-lg">
              <div className="flex space-x-1">
                <div className="w-2 h-2 bg-gray-600 rounded-full animate-bounce" />
                <div className="w-2 h-2 bg-gray-600 rounded-full animate-bounce delay-100" />
                <div className="w-2 h-2 bg-gray-600 rounded-full animate-bounce delay-200" />
              </div>
            </div>
          </div>
        )}
        <div ref={scrollRef} />
      </div>

      <form onSubmit={handleSubmit} className="flex gap-2">
        <input
          type="text"
          value={userInput}
          onChange={(e) => setUserInput(e.target.value)}
          placeholder="Wpisz odpowiedź..."
          disabled={loading}
          className="flex-1 px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-emerald-500"
        />
        <button
          type="submit"
          disabled={loading || !userInput.trim()}
          className="px-4 py-2 bg-emerald-600 text-white rounded-lg hover:bg-emerald-700 disabled:bg-gray-400"
        >
          Wyślij
        </button>
      </form>

      {clarificationRound > 0 && (
        <p className="text-xs text-gray-500 mt-2">
          Runda: {clarificationRound}/3
        </p>
      )}
    </div>
  )
}
