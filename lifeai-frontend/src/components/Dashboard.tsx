import { useState } from 'react'
import { useSendEvent, useTimeline, useRecommendations } from '../hooks'
import { Event } from '../types'
import { Timeline } from './Timeline'

export const Dashboard = () => {
  const [inputValue, setInputValue] = useState('')
  
  const { send: sendEvent, status: sendStatus, error: sendError } = useSendEvent()
  const { fetch: fetchTimeline } = useTimeline(50)
  const { fetch: fetchRecommendations, status: recStatus, data: recData, error: recError } = useRecommendations()

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

  return (
    <div className="min-h-screen bg-gray-50 py-8 px-4 sm:px-6 lg:px-8">
      <div className="max-w-6xl mx-auto">
        <h1 className="text-4xl font-bold text-gray-900 mb-8">Dashboard LifeAI</h1>
        
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div className="lg:col-span-2">
            <div className="bg-white rounded-lg shadow-md p-6 mb-6">
              <h2 className="text-xl font-semibold text-gray-900 mb-4">Dodaj zdarzenie</h2>
              
              {sendError && (
                <div className="mb-4 p-4 bg-red-50 border border-red-200 rounded-lg">
                  <p className="text-sm text-red-700">Błąd: {sendError.message}</p>
                </div>
              )}
              
              <div className="space-y-4">
                <div>
                  <label htmlFor="event-input" className="block text-sm font-medium text-gray-700 mb-2">
                    Opisz swoje doświadczenie
                  </label>
                  <textarea
                    id="event-input"
                    value={inputValue}
                    onChange={(e) => setInputValue(e.target.value)}
                    onKeyPress={handleKeyPress}
                    placeholder="Np. Właśnie skończyłem 30 minut treningu cardio..."
                    className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-transparent resize-none"
                    rows={4}
                    disabled={sendStatus === 'pending'}
                  />
                  <p className="mt-2 text-xs text-gray-500">Wskazówka: Naciśnij Ctrl+Enter aby szybko dodać zdarzenie</p>
                </div>
                
                <button
                  onClick={handleAddEvent}
                  disabled={!inputValue.trim() || sendStatus === 'pending'}
                  className="w-full bg-indigo-600 hover:bg-indigo-700 disabled:bg-gray-400 text-white font-semibold py-3 px-4 rounded-lg transition-colors"
                >
                  {sendStatus === 'pending' ? 'Wysyłanie...' : 'Dodaj zdarzenie'}
                </button>
              </div>
            </div>

            <Timeline limit={50} />
          </div>

          <div className="lg:col-span-1">
            <div className="bg-white rounded-lg shadow-md p-6 sticky top-6">
              <div className="flex justify-between items-center mb-4">
                <h2 className="text-xl font-semibold text-gray-900">Rekomendacje</h2>
                <button
                  onClick={() => fetchRecommendations()}
                  disabled={recStatus === 'pending'}
                  className="text-sm px-2 py-1 text-gray-400 hover:text-gray-600 disabled:opacity-50"
                >
                  ↻
                </button>
              </div>
              
              {recStatus === 'pending' && (
                <div className="text-center py-8">
                  <div className="inline-block">
                    <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-indigo-600"></div>
                  </div>
                  <p className="text-gray-500 text-xs mt-2">Ładowanie...</p>
                </div>
              )}
              
              {recError && (
                <div className="p-3 bg-red-50 border border-red-200 rounded-lg mb-4">
                  <p className="text-xs text-red-700">Błąd: {recError.message}</p>
                </div>
              )}
              
              {recStatus === 'success' && recData && recData.length === 0 && (
                <div className="text-center py-6">
                  <p className="text-sm text-gray-500">Brak rekomendacji</p>
                </div>
              )}
              
              {recStatus === 'success' && recData && (
                <div className="space-y-4">
                  {recData.map((rec) => (
                    <div key={rec.id} className={`p-4 rounded-lg border ${
                      rec.priority === 'high' ? 'bg-red-50 border-red-100' :
                      rec.priority === 'medium' ? 'bg-yellow-50 border-yellow-100' :
                      'bg-green-50 border-green-100'
                    } hover:shadow-md transition-shadow`}>
                      <div className="flex gap-3">
                        <div className="flex-1">
                          <div className="flex items-center gap-2">
                            <h3 className="font-semibold text-gray-900 text-sm">{rec.suggestion}</h3>
                            <span className={`text-xs font-medium px-2 py-0.5 rounded ${
                              rec.priority === 'high' ? 'bg-red-100 text-red-700' :
                              rec.priority === 'medium' ? 'bg-yellow-100 text-yellow-700' :
                              'bg-green-100 text-green-700'
                            }`}>
                              {rec.priority === 'high' ? 'Wysoki' : rec.priority === 'medium' ? 'Średni' : 'Niski'}
                            </span>
                          </div>
                          <p className="text-xs text-gray-600 mt-1">{rec.category}</p>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              )}

              <div className="mt-6 pt-6 border-t border-gray-200">
                <p className="text-xs text-gray-500 text-center">
                  Rekomendacje bazują na Twoich danych z ostatnich 7 dni
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
