import { useState, useEffect } from 'react'
import './App.css'
import { apiClient } from './services/api'

function App() {
  const [message, setMessage] = useState<string>('')
  const [error, setError] = useState<string>('')
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    fetchGreeting()
  }, [])

  const fetchGreeting = async () => {
    setLoading(true)
    setError('')
    try {
      const response = await apiClient.get('/health')
      const status = response.data?.status || 'Connected'
      setMessage(`Backend Status: ${status}`)
    } catch (err) {
      setError('Failed to connect to backend')
      console.error(err)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100 flex items-center justify-center p-4">
      <div className="bg-white rounded-lg shadow-2xl p-8 max-w-md w-full">
        <h1 className="text-3xl font-bold text-gray-800 mb-6 text-center">LifeAI</h1>
        
        <div className="bg-gray-50 rounded-lg p-6 mb-6 min-h-24 flex items-center justify-center">
          {loading && (
            <p className="text-gray-500">Loading...</p>
          )}
          {error && (
            <p className="text-red-600 text-center">{error}</p>
          )}
          {message && !loading && !error && (
            <p className="text-gray-700 text-center font-medium">{message}</p>
          )}
        </div>

        <button
          onClick={fetchGreeting}
          disabled={loading}
          className="w-full bg-indigo-600 hover:bg-indigo-700 disabled:bg-gray-400 text-white font-semibold py-2 px-4 rounded-lg transition-colors"
        >
          {loading ? 'Loading...' : 'Refresh'}
        </button>

        <div className="mt-8 pt-6 border-t border-gray-200">
          <p className="text-sm text-gray-600 text-center">
            Backend connection status
          </p>
          <p className="text-xs text-gray-500 text-center mt-2">
            Connected to: http://localhost:8080
          </p>
        </div>
      </div>
    </div>
  )
}

export default App
