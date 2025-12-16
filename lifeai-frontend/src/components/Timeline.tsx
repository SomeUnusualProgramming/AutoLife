import { useTimeline } from '../hooks'
import { TimelineEntry } from '../types'

interface TimelineGroup {
  date: string
  entries: TimelineEntry[]
}

const groupEntriesByDay = (entries: TimelineEntry[]): TimelineGroup[] => {
  const grouped = new Map<string, TimelineEntry[]>()

  entries.forEach((entry) => {
    const date = new Date(entry.createdAt).toLocaleDateString('pl-PL')
    if (!grouped.has(date)) {
      grouped.set(date, [])
    }
    grouped.get(date)!.push(entry)
  })

  return Array.from(grouped.entries())
    .map(([date, entries]) => ({
      date,
      entries: entries.sort((a, b) => 
        new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
      ),
    }))
    .sort((a, b) => {
      const dateA = new Date(a.entries[0].createdAt)
      const dateB = new Date(b.entries[0].createdAt)
      return dateB.getTime() - dateA.getTime()
    })
}

const formatTime = (dateStr: string): string => {
  return new Date(dateStr).toLocaleTimeString('pl-PL', {
    hour: '2-digit',
    minute: '2-digit',
  })
}

const formatDate = (dateStr: string): string => {
  return new Date(dateStr).toLocaleDateString('pl-PL', {
    weekday: 'long',
    month: 'long',
    day: 'numeric',
  })
}

const isToday = (dateStr: string): boolean => {
  const today = new Date().toLocaleDateString('pl-PL')
  return dateStr === today
}

interface TimelineProps {
  limit?: number
  offset?: number
}

export const Timeline: React.FC<TimelineProps> = ({ limit = 50, offset = 0 }) => {
  const { fetch: fetchTimeline, status: timelineStatus, data: timelineData, error: timelineError } = useTimeline(limit, offset)

  const displayGroups = timelineData?.entries ? groupEntriesByDay(timelineData.entries) : []

  return (
    <div className="bg-white rounded-lg shadow-md p-6">
      <div className="flex justify-between items-center mb-4">
        <h2 className="text-xl font-semibold text-gray-900">Timeline</h2>
        <button
          onClick={() => fetchTimeline()}
          disabled={timelineStatus === 'pending'}
          className="text-sm px-3 py-1 bg-gray-100 hover:bg-gray-200 rounded disabled:opacity-50"
        >
          {timelineStatus === 'pending' ? 'Ładowanie...' : 'Odśwież'}
        </button>
      </div>

      {timelineStatus === 'pending' && (
        <div className="text-center py-8">
          <div className="inline-block">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600"></div>
          </div>
          <p className="text-gray-500 mt-2">Ładowanie timeline...</p>
        </div>
      )}

      {timelineError && (
        <div className="p-4 bg-red-50 border border-red-200 rounded-lg mb-4">
          <p className="text-sm text-red-700">Błąd: {timelineError.message}</p>
        </div>
      )}

      {timelineStatus === 'success' && displayGroups.length === 0 ? (
        <div className="text-center py-8">
          <p className="text-gray-500">Brak zdarzeń. Dodaj swoje pierwsze zdarzenie!</p>
        </div>
      ) : timelineStatus === 'success' && (
        <div className="space-y-8">
          {displayGroups.map((group) => (
            <div key={group.date} className="timeline-day-group">
              <div className="flex items-center gap-3 mb-4">
                <h3 className="text-lg font-semibold text-gray-900">
                  {formatDate(group.entries[0].createdAt)}
                </h3>
                {isToday(group.date) && (
                  <span className="inline-block px-2 py-1 text-xs font-medium bg-indigo-100 text-indigo-700 rounded">
                    Dziś
                  </span>
                )}
              </div>

              <div className="relative space-y-4 ml-4">
                {group.entries.map((entry, index) => (
                  <div key={entry.id} className="relative pb-4">
                    {index !== group.entries.length - 1 && (
                      <div className="absolute left-3 top-8 w-0.5 h-12 bg-indigo-200"></div>
                    )}

                    <div className="flex gap-4">
                      <div className="flex-shrink-0 relative z-10">
                        <div className="flex items-center justify-center h-7 w-7 rounded-full bg-indigo-600">
                          <div className="h-3 w-3 rounded-full bg-white"></div>
                        </div>
                      </div>

                      <div className="flex-1">
                        <div className="bg-gray-50 rounded-lg p-4">
                          <p className="text-gray-900 font-medium">{entry.event.description}</p>
                          <div className="flex gap-2 text-xs text-gray-500 mt-2">
                            <span>{formatTime(entry.createdAt)}</span>
                            <span>•</span>
                            <span>Ważność: {entry.importance}/10</span>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
