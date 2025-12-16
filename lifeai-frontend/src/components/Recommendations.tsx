import { useRecommendations } from '../hooks'
import { Recommendation } from '../types'

interface RecommendationsProps {
  category?: string
  compact?: boolean
}

const getPriorityColor = (priority: 'low' | 'medium' | 'high') => {
  switch (priority) {
    case 'high':
      return 'bg-red-50 border-red-100'
    case 'medium':
      return 'bg-yellow-50 border-yellow-100'
    case 'low':
      return 'bg-green-50 border-green-100'
  }
}

const getPriorityBadgeColor = (priority: 'low' | 'medium' | 'high') => {
  switch (priority) {
    case 'high':
      return 'bg-red-100 text-red-700'
    case 'medium':
      return 'bg-yellow-100 text-yellow-700'
    case 'low':
      return 'bg-green-100 text-green-700'
  }
}

const getStatusColor = (status: 'PLANNED' | 'DONE') => {
  return status === 'DONE'
    ? 'bg-blue-100 text-blue-700'
    : 'bg-gray-100 text-gray-700'
}

const getStatusLabel = (status: 'PLANNED' | 'DONE') => {
  return status === 'DONE' ? 'Wykonane' : 'Zaplanowane'
}

const RecommendationItem = ({
  recommendation,
  onStatusChange,
}: {
  recommendation: Recommendation
  onStatusChange: (id: string, newStatus: 'PLANNED' | 'DONE') => void
}) => {
  const handleStatusToggle = () => {
    const newStatus = recommendation.status === 'DONE' ? 'PLANNED' : 'DONE'
    onStatusChange(recommendation.id, newStatus)
  }

  return (
    <div
      className={`p-4 rounded-lg border transition-all hover:shadow-md ${getPriorityColor(
        recommendation.priority
      )} ${recommendation.status === 'DONE' ? 'opacity-75' : ''}`}
    >
      <div className="flex gap-3">
        <button
          onClick={handleStatusToggle}
          className="flex-shrink-0 mt-0.5"
          title={`Zmień status na ${
            recommendation.status === 'DONE' ? 'Zaplanowane' : 'Wykonane'
          }`}
        >
          <div
            className={`w-5 h-5 rounded border-2 flex items-center justify-center transition-colors ${
              recommendation.status === 'DONE'
                ? 'bg-blue-600 border-blue-600'
                : 'border-gray-400 hover:border-blue-600'
            }`}
          >
            {recommendation.status === 'DONE' && (
              <span className="text-white text-sm">✓</span>
            )}
          </div>
        </button>

        <div className="flex-1">
          <div className="flex items-start justify-between gap-2">
            <div className="flex-1">
              <h4
                className={`font-semibold text-sm ${
                  recommendation.status === 'DONE'
                    ? 'line-through text-gray-600'
                    : 'text-gray-900'
                }`}
              >
                {recommendation.suggestion}
              </h4>
              <p className="text-xs text-gray-600 mt-1">{recommendation.category}</p>
            </div>

            <div className="flex gap-1 flex-shrink-0">
              <span className={`text-xs font-medium px-2 py-0.5 rounded ${getPriorityBadgeColor(recommendation.priority)}`}>
                {recommendation.priority === 'high'
                  ? 'Wysoki'
                  : recommendation.priority === 'medium'
                    ? 'Średni'
                    : 'Niski'}
              </span>
              <span className={`text-xs font-medium px-2 py-0.5 rounded ${getStatusColor(recommendation.status)}`}>
                {getStatusLabel(recommendation.status)}
              </span>
            </div>
          </div>

          {recommendation.aiGenerated && (
            <div className="mt-2 inline-block">
              <span className="text-xs bg-purple-100 text-purple-700 px-2 py-0.5 rounded font-medium">
                ✨ AI
              </span>
            </div>
          )}

          {recommendation.actionUrl && (
            <div className="mt-2">
              <a
                href={recommendation.actionUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="text-xs text-indigo-600 hover:text-indigo-700 font-medium"
              >
                Otwórz →
              </a>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

export const Recommendations = ({
  category,
  compact = false,
}: RecommendationsProps) => {
  const { fetch, status, data, error, markDone, markPlanned } =
    useRecommendations(category)

  const handleStatusChange = async (id: string, newStatus: 'PLANNED' | 'DONE') => {
    try {
      if (newStatus === 'DONE') {
        await markDone(id)
      } else {
        await markPlanned(id)
      }
    } catch (err) {
      console.error('Failed to update recommendation status:', err)
    }
  }

  if (compact) {
    return (
      <div className="space-y-2">
        {status === 'pending' && (
          <div className="text-center py-4">
            <div className="inline-block">
              <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-indigo-600"></div>
            </div>
            <p className="text-gray-500 text-xs mt-1">Ładowanie...</p>
          </div>
        )}

        {error && (
          <div className="p-2 bg-red-50 border border-red-200 rounded-lg">
            <p className="text-xs text-red-700">Błąd: {error.message}</p>
          </div>
        )}

        {status === 'success' && data && data.length === 0 && (
          <div className="text-center py-3">
            <p className="text-xs text-gray-500">Brak rekomendacji</p>
          </div>
        )}

        {status === 'success' && data && (
          <div className="space-y-2">
            {data.map((rec) => (
              <RecommendationItem
                key={rec.id}
                recommendation={rec}
                onStatusChange={handleStatusChange}
              />
            ))}
          </div>
        )}
      </div>
    )
  }

  return (
    <div className="bg-white rounded-lg shadow-md p-6">
      <div className="flex justify-between items-center mb-6">
        <h2 className="text-2xl font-semibold text-gray-900">Rekomendacje</h2>
        <button
          onClick={() => fetch()}
          disabled={status === 'pending'}
          className="text-sm px-3 py-1 text-gray-400 hover:text-gray-600 disabled:opacity-50 transition-colors"
          title="Odśwież rekomendacje"
        >
          ↻
        </button>
      </div>

      {status === 'pending' && (
        <div className="text-center py-12">
          <div className="inline-block">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600"></div>
          </div>
          <p className="text-gray-500 text-sm mt-3">Ładowanie rekomendacji...</p>
        </div>
      )}

      {error && (
        <div className="p-4 bg-red-50 border border-red-200 rounded-lg mb-4">
          <p className="text-sm text-red-700 font-medium">Błąd</p>
          <p className="text-sm text-red-600 mt-1">{error.message}</p>
        </div>
      )}

      {status === 'success' && data && data.length === 0 && (
        <div className="text-center py-12">
          <p className="text-gray-500 text-lg">Brak rekomendacji</p>
          <p className="text-gray-400 text-sm mt-2">
            Rekomendacje pojawią się gdy prześledzisz więcej zdarzeń
          </p>
        </div>
      )}

      {status === 'success' && data && (
        <div className="space-y-3">
          <div className="flex gap-2 mb-4">
            <button
              className="text-xs px-3 py-1 rounded-full border border-gray-300 text-gray-700 hover:bg-gray-50 transition-colors"
              title="Filtr: wszystkie"
            >
              Wszystkie ({data.length})
            </button>
            <button
              className="text-xs px-3 py-1 rounded-full border border-gray-300 text-gray-700 hover:bg-gray-50 transition-colors"
              title="Filtr: zaplanowane"
            >
              Zaplanowane ({data.filter((r) => r.status === 'PLANNED').length})
            </button>
            <button
              className="text-xs px-3 py-1 rounded-full border border-gray-300 text-gray-700 hover:bg-gray-50 transition-colors"
              title="Filtr: wykonane"
            >
              Wykonane ({data.filter((r) => r.status === 'DONE').length})
            </button>
          </div>

          {data.map((rec) => (
            <RecommendationItem
              key={rec.id}
              recommendation={rec}
              onStatusChange={handleStatusChange}
            />
          ))}
        </div>
      )}

      <div className="mt-6 pt-6 border-t border-gray-200">
        <p className="text-xs text-gray-500 text-center">
          Rekomendacje bazują na Twoich danych z ostatnich 7 dni
        </p>
        <p className="text-xs text-gray-400 text-center mt-1">
          ✨ Oznaczenia AI wskazują rekomendacje generowane przez algorytm
        </p>
      </div>
    </div>
  )
}
