import { useState, useCallback } from 'react'
import { useSendEvent, useTimeline, useRecommendations, useUser } from '../hooks'
import { Event } from '../types'
import { Timeline } from './Timeline'
import { AudioRecorder } from './AudioRecorder'

export const Dashboard = () => {
  const [inputValue, setInputValue] = useState('')
  const [transcriptionRecommendations, setTranscriptionRecommendations] = useState<any[]>([])
  
  const { userId, isLoading: userLoading } = useUser()
  const { send: sendEvent, status: sendStatus, error: sendError } = useSendEvent()
  const { fetch: fetchTimeline } = useTimeline(50, 0, userId || undefined)
  const { fetch: fetchRecommendations, status: recStatus, data: recData, error: recError } = useRecommendations(undefined, userId || undefined)

  const handleAddEvent = async () => {
    if (inputValue.trim()) {
      try {
        const newEvent: Event = {
          type: 'user_input',
          description: inputValue,
          timestamp: new Date().toISOString()
        }
        await sendEvent(newEvent)
        setInputValue('')
        await fetchTimeline()
      } catch (error) {
        console.error('Failed to send event:', error)
      }
    }
  }

  const handleKeyPress = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && e.ctrlKey) {
      handleAddEvent()
    }
  }

  const handleTranscriptionComplete = (text: string) => {
    setInputValue((prev) => (prev ? prev + ' ' + text : text))
  }

  const handleEventCreated = useCallback(async (event: any) => {
    console.log('Event created from transcription:', event)
    await fetchTimeline()
  }, [fetchTimeline])

  const handleRecommendationsReceived = useCallback((recommendations: any[]) => {
    console.log('Recommendations received from transcription:', recommendations)
    setTranscriptionRecommendations(recommendations)
    if (recommendations.length > 0) {
      fetchRecommendations()
    }
  }, [fetchRecommendations])

  if (userLoading) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-gray-50 via-white to-emerald-50 py-8 px-4 flex items-center justify-center">
        <div className="text-center">
          <div className="inline-block animate-spin rounded-full h-8 w-8 border-2 border-emerald-200 border-t-emerald-600 mb-4"></div>
          <p className="text-gray-600">Loading LifeAI...</p>
        </div>
      </div>
    )
  }

  const allRecommendations = [
    ...transcriptionRecommendations.map(rec => ({
      id: String(rec.id),
      category: rec.type || 'Health',
      suggestion: rec.text,
      priority: (rec.priority?.toLowerCase() || 'medium') as 'low' | 'medium' | 'high',
      status: rec.isApplied ? 'DONE' : 'PLANNED' as const,
    })),
    ...(recData?.map(rec => ({
      ...rec,
      suggestion: rec.suggestion || (rec as any).text,
      category: rec.category || (rec as any).type,
      priority: (rec.priority?.toLowerCase() || 'medium') as 'low' | 'medium' | 'high'
    })) || [])
  ]

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-50 via-white to-emerald-50 py-8 px-4 sm:px-6 lg:px-8">
      <div className="max-w-6xl mx-auto">
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-900 mb-2">LifeAI</h1>
          <p className="text-gray-600 text-sm">Track your events and get personalized suggestions for a healthier lifestyle {userId && `(User #${userId})`}</p>
        </div>
        
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div className="lg:col-span-2">
            <AudioRecorder 
              onTranscriptionComplete={handleTranscriptionComplete}
              onEventCreated={handleEventCreated}
              onRecommendationsReceived={handleRecommendationsReceived}
            />
            
            <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6 mb-6">
              <h2 className="text-lg font-semibold text-gray-900 mb-4">Add New Event</h2>
              
              {sendError && (
                <div className="mb-4 p-4 bg-rose-50 border border-rose-200 rounded-lg">
                  <p className="text-sm text-rose-700">Error: {sendError.message}</p>
                </div>
              )}
              
              <div className="space-y-4">
                <div>
                  <label htmlFor="event-input" className="block text-sm font-medium text-gray-700 mb-2">
                    Describe what happened
                  </label>
                  <textarea
                    id="event-input"
                    value={inputValue}
                    onChange={(e) => setInputValue(e.target.value)}
                    onKeyPress={handleKeyPress}
                    placeholder="E.g., Just finished 30 minutes of running in the fresh air..."
                    className="w-full px-4 py-3 border border-gray-200 rounded-lg focus:ring-2 focus:ring-emerald-500 focus:border-transparent resize-none bg-white transition-colors"
                    rows={4}
                    disabled={sendStatus === 'pending'}
                  />
                  <p className="mt-2 text-xs text-gray-500">💡 Tip: Press Ctrl+Enter or use voice recording</p>
                </div>
                
                <button
                  onClick={handleAddEvent}
                  disabled={!inputValue.trim() || sendStatus === 'pending'}
                  className="w-full bg-emerald-600 hover:bg-emerald-700 disabled:bg-gray-400 text-white font-semibold py-3 px-4 rounded-lg transition-colors"
                >
                  {sendStatus === 'pending' ? 'Sending...' : '✓ Add Event'}
                </button>
              </div>
            </div>

            <Timeline limit={50} userId={userId || undefined} />
          </div>

          <div className="lg:col-span-1">
            <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6 sticky top-6">
              <div className="flex justify-between items-center mb-5">
                <div>
                  <h2 className="text-lg font-semibold text-gray-900">💡 AI Suggestions</h2>
                  <p className="text-xs text-gray-500 mt-1">Based on your activity</p>
                </div>
                <button
                  onClick={() => fetchRecommendations()}
                  disabled={recStatus === 'pending'}
                  className="text-sm px-3 py-1.5 text-gray-600 hover:text-gray-900 hover:bg-gray-50 rounded-lg transition-colors disabled:opacity-50"
                >
                  ↻
                </button>
              </div>
              
              {recStatus === 'pending' && (
                <div className="text-center py-10">
                  <div className="inline-block">
                    <div className="animate-spin rounded-full h-6 w-6 border-2 border-emerald-200 border-t-emerald-600"></div>
                  </div>
                  <p className="text-gray-500 text-xs mt-3">Analyzing your data...</p>
                </div>
              )}
              
              {recError && (
                <div className="p-3 bg-rose-50 border border-rose-200 rounded-lg mb-4">
                  <p className="text-xs text-rose-700">Error: {recError.message}</p>
                </div>
              )}
              
              {recStatus === 'success' && allRecommendations.length === 0 && (
                <div className="text-center py-8 bg-gray-50 rounded-lg">
                  <p className="text-sm text-gray-500">No suggestions yet</p>
                  <p className="text-xs text-gray-400 mt-1">Add more events to get AI suggestions</p>
                </div>
              )}
              
              {allRecommendations.length > 0 && (
                <div className="space-y-3">
                  {allRecommendations.slice(0, 5).map((rec) => (
                    <div key={rec.id} className={`p-3.5 rounded-lg border transition-all ${
                      rec.priority === 'high' ? 'bg-rose-50 border-rose-100 hover:border-rose-200' :
                      rec.priority === 'medium' ? 'bg-amber-50 border-amber-100 hover:border-amber-200' :
                      'bg-emerald-50 border-emerald-100 hover:border-emerald-200'
                    }`}>
                      <div className="flex gap-2">
                        <div className="flex-1">
                          <div className="flex items-center gap-2 mb-1">
                            <span className="text-lg">✨</span>
                            <h3 className="font-medium text-gray-900 text-sm flex-1">{rec.suggestion}</h3>
                          </div>
                          <div className="flex gap-2 items-center">
                            <span className="text-xs text-gray-600 font-medium">{rec.category}</span>
                            <span className={`text-xs font-medium px-2 py-0.5 rounded-full ${
                              rec.priority === 'high' ? 'bg-rose-100 text-rose-700' :
                              rec.priority === 'medium' ? 'bg-amber-100 text-amber-700' :
                              'bg-emerald-100 text-emerald-700'
                            }`}>
                              {rec.priority === 'high' ? 'Ważne' : rec.priority === 'medium' ? 'Średnie' : 'Podstawowe'}
                            </span>
                          </div>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              )}

              <div className="mt-6 pt-4 border-t border-gray-100">
                <p className="text-xs text-gray-500 text-center">
                  Recommendations are generated based on your activity
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
