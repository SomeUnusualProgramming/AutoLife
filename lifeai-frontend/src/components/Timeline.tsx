import { useEffect } from 'react'
import { useTimeline } from '../hooks'
import { TimelineEntry } from '../types'
import { SkeletonLoader } from './SkeletonLoader'

interface TimelineGroup {
  date: string
  entries: TimelineEntry[]
}

interface TimePeriodGroup {
  period: string
  icon: string
  entries: TimelineEntry[]
}

const groupEntriesByDay = (entries: TimelineEntry[]): TimelineGroup[] => {
  const grouped = new Map<string, TimelineEntry[]>()

  entries.forEach((entry) => {
    const date = new Date(entry.createdAt).toLocaleDateString('en-US')
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

const getTimePeriod = (dateStr: string): { period: string; icon: string } => {
  const hour = new Date(dateStr).getHours()
  if (hour >= 5 && hour < 12) {
    return { period: 'Morning', icon: '🌅' }
  } else if (hour >= 12 && hour < 17) {
    return { period: 'Afternoon', icon: '☀️' }
  } else if (hour >= 17 && hour < 21) {
    return { period: 'Evening', icon: '🌆' }
  } else {
    return { period: 'Night', icon: '🌙' }
  }
}

const getEventHealthStatus = (description: string): { isHealthy: boolean; reason: string } => {
  const healthyKeywords = [
    'trening', 'ćwiczenie', 'sport', 'bieg', 'pływanie', 'joga', 'medytacja',
    'owoce', 'warzywa', 'sałatka', 'woda', 'sen', 'odpoczynek', 'spacer',
    'zdrowy', 'diet', 'fitness', 'zielona herbata', 'sobie'
  ]
  
  const unhealthyKeywords = [
    'cola', 'burger', 'pizza', 'fast food', 'czekolada', 'cukier', 'słodycz',
    'alkohol', 'paliwo', 'papieros', 'stres', 'bezsenność', 'zmęczenie',
    'nieszczelny', 'niezdrowy', 'śmieci'
  ]
  
  const lowerDesc = description.toLowerCase()
  
  const hasHealthy = healthyKeywords.some(kw => lowerDesc.includes(kw))
  const hasUnhealthy = unhealthyKeywords.some(kw => lowerDesc.includes(kw))
  
  if (hasUnhealthy && !hasHealthy) {
    return { isHealthy: false, reason: 'Unhealthy' }
  }
  if (hasHealthy && !hasUnhealthy) {
    return { isHealthy: true, reason: 'Healthy' }
  }
  
  return { isHealthy: true, reason: 'Neutral' }
}

const groupEntriesByTimePeriod = (entries: TimelineEntry[]): TimePeriodGroup[] => {
  const grouped = new Map<string, TimelineEntry[]>()

  entries.forEach((entry) => {
    const { period } = getTimePeriod(entry.createdAt)
    if (!grouped.has(period)) {
      grouped.set(period, [])
    }
    grouped.get(period)!.push(entry)
  })

  const periodOrder = ['Morning', 'Afternoon', 'Evening', 'Night']
  const result: TimePeriodGroup[] = []

  periodOrder.forEach((period) => {
    if (grouped.has(period)) {
      const { icon } = getTimePeriod(period)
      result.push({
        period,
        icon,
        entries: grouped.get(period)!,
      })
    }
  })

  return result
}

const formatTime = (dateStr: string): string => {
  return new Date(dateStr).toLocaleTimeString('en-US', {
    hour: '2-digit',
    minute: '2-digit',
  })
}

const formatDate = (dateStr: string): string => {
  return new Date(dateStr).toLocaleDateString('en-US', {
    weekday: 'long',
    month: 'long',
    day: 'numeric',
  })
}

const isToday = (dateStr: string): boolean => {
  const today = new Date().toLocaleDateString('en-US')
  return dateStr === today
}

interface TimelineProps {
  limit?: number
  offset?: number
  userId?: number
  refreshKey?: number
}

export const Timeline: React.FC<TimelineProps> = ({ limit = 50, offset = 0, userId, refreshKey = 0 }) => {
  const { fetch: fetchTimeline, status: timelineStatus, data: timelineData, error: timelineError } = useTimeline(limit, offset, userId)
  
  useEffect(() => {
    if (userId) {
      fetchTimeline()
    }
  }, [refreshKey, userId, fetchTimeline])

  const displayGroups = timelineData?.entries ? groupEntriesByDay(timelineData.entries) : []

  return (
    <div className="bg-white rounded-lg shadow-sm border border-gray-100 p-6">
      <div className="flex justify-between items-center mb-6">
        <h2 className="text-2xl font-semibold text-gray-900">Daily Journal</h2>
        <button
          onClick={() => fetchTimeline()}
          disabled={timelineStatus === 'pending'}
          className="text-sm px-3 py-1.5 text-gray-600 hover:text-gray-900 hover:bg-gray-50 rounded-lg transition-colors disabled:opacity-50"
        >
          {timelineStatus === 'pending' ? '↻ Loading...' : '↻ Refresh'}
        </button>
      </div>

      {timelineStatus === 'pending' && (
        <div className="py-4">
          <SkeletonLoader type="timeline" count={2} />
        </div>
      )}

      {timelineError && (
        <div className="p-4 bg-rose-50 border border-rose-200 rounded-lg mb-4">
          <p className="text-sm text-rose-700">Error: {timelineError.message}</p>
        </div>
      )}

      {timelineStatus === 'success' && displayGroups.length === 0 && (
        <div className="text-center py-12">
          <p className="text-4xl mb-4">📅</p>
          <p className="text-gray-900 text-lg font-medium mb-1">No events</p>
          <p className="text-gray-600 text-sm mb-4">Add your first event to start tracking activities</p>
          <p className="text-gray-500 text-xs">You can record voice or type text in the field below</p>
        </div>
      )}

      {timelineStatus === 'success' && displayGroups.length > 0 && (
        <div className="space-y-8">
          {displayGroups.map((group) => (
            <div key={group.date} className="timeline-day-group">
              <div className="flex items-center gap-3 mb-6 pb-3 border-b border-gray-100">
                <h3 className="text-base font-semibold text-gray-900">
                  {formatDate(group.entries[0].createdAt)}
                </h3>
                {isToday(group.date) && (
                  <span className="inline-block px-2.5 py-1 text-xs font-medium bg-emerald-100 text-emerald-700 rounded-full">
                    Today
                  </span>
                )}
              </div>

              <div className="space-y-6">
                {groupEntriesByTimePeriod(group.entries).map((periodGroup) => (
                  <div key={periodGroup.period} className="space-y-3">
                    <div className="flex items-center gap-2 text-sm font-medium text-gray-700">
                      <span className="text-xl">{periodGroup.icon}</span>
                      <span>{periodGroup.period}</span>
                    </div>

                    <div className="relative space-y-3 ml-8">
                      {periodGroup.entries.map((entry, index) => {
                        const healthStatus = getEventHealthStatus(entry.event.description)
                        const isHealthy = healthStatus.isHealthy
                        
                        return (
                          <div key={entry.id} className="relative">
                            {index !== periodGroup.entries.length - 1 && (
                              <div className="absolute left-1.5 top-6 w-0.5 h-10 bg-gray-200"></div>
                            )}

                            <div className="flex gap-3">
                              <div className="flex-shrink-0 relative z-10 mt-0.5">
                                <div className={`flex items-center justify-center h-3 w-3 rounded-full ${
                                  isHealthy ? 'bg-emerald-500' : 'bg-amber-500'
                                }`}></div>
                              </div>

                              <div className="flex-1 min-w-0">
                                <div className={`rounded-lg p-3.5 border transition-all ${
                                  isHealthy 
                                    ? 'bg-emerald-50 border-emerald-100 hover:border-emerald-200' 
                                    : 'bg-amber-50 border-amber-100 hover:border-amber-200'
                                }`}>
                                  <div className="flex items-start justify-between gap-2 mb-2">
                                    <p className="text-gray-900 font-medium text-sm flex-1">{entry.event.description}</p>
                                    <span className={`flex-shrink-0 text-xs font-medium px-2 py-1 rounded-full whitespace-nowrap ${
                                      isHealthy
                                        ? 'bg-emerald-100 text-emerald-700'
                                        : 'bg-amber-100 text-amber-700'
                                    }`}>
                                      {healthStatus.reason}
                                    </span>
                                  </div>
                                  <div className="flex gap-2 text-xs text-gray-600">
                                    <span>{formatTime(entry.createdAt)}</span>
                                    <span>•</span>
                                    <span>Importance: <span className="font-medium">{entry.importance}</span>/10</span>
                                  </div>
                                </div>
                              </div>
                            </div>
                          </div>
                        )
                      })}
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
