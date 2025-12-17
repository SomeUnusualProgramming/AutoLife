import { useState, useMemo } from 'react'
import { TimelineEntry } from '../types'

interface CalendarProps {
  entries?: TimelineEntry[]
  onDateSelect?: (date: Date) => void
}

export const Calendar: React.FC<CalendarProps> = ({ entries = [], onDateSelect }) => {
  const [currentDate, setCurrentDate] = useState(new Date())

  const eventsByDate = useMemo(() => {
    const map = new Map<string, number>()
    entries.forEach((entry) => {
      const date = new Date(entry.createdAt).toLocaleDateString('en-US')
      map.set(date, (map.get(date) || 0) + 1)
    })
    return map
  }, [entries])

  const daysInMonth = (date: Date) => new Date(date.getFullYear(), date.getMonth() + 1, 0).getDate()
  const firstDayOfMonth = (date: Date) => new Date(date.getFullYear(), date.getMonth(), 1).getDay()

  const monthDays = daysInMonth(currentDate)
  const firstDay = firstDayOfMonth(currentDate)
  const previousMonth = () => setCurrentDate(new Date(currentDate.getFullYear(), currentDate.getMonth() - 1))
  const nextMonth = () => setCurrentDate(new Date(currentDate.getFullYear(), currentDate.getMonth() + 1))

  const monthName = currentDate.toLocaleDateString('en-US', { month: 'long', year: 'numeric' })
  const today = new Date().toLocaleDateString('en-US')

  const days: (number | null)[] = []
  for (let i = 0; i < firstDay; i++) {
    days.push(null)
  }
  for (let i = 1; i <= monthDays; i++) {
    days.push(i)
  }

  const handleDateClick = (day: number) => {
    const selectedDate = new Date(currentDate.getFullYear(), currentDate.getMonth(), day)
    onDateSelect?.(selectedDate)
  }

  return (
    <div className="bg-white rounded-lg shadow-sm border border-gray-100 p-4">
      <div className="flex items-center justify-between mb-4">
        <button
          onClick={previousMonth}
          className="p-1.5 hover:bg-gray-100 rounded-lg transition-colors"
          aria-label="Previous month"
        >
          ←
        </button>
        <h3 className="text-sm font-semibold text-gray-900 capitalize">{monthName}</h3>
        <button
          onClick={nextMonth}
          className="p-1.5 hover:bg-gray-100 rounded-lg transition-colors"
          aria-label="Next month"
        >
          →
        </button>
      </div>

      <div className="grid grid-cols-7 gap-1 mb-2">
        {['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'].map((day) => (
          <div key={day} className="text-center text-xs font-medium text-gray-600 py-2">
            {day}
          </div>
        ))}
      </div>

      <div className="grid grid-cols-7 gap-1">
        {days.map((day, index) => {
          const dateStr = day
            ? new Date(currentDate.getFullYear(), currentDate.getMonth(), day).toLocaleDateString('en-US')
            : null
          const eventCount = dateStr ? eventsByDate.get(dateStr) || 0 : 0
          const isToday = dateStr === today
          const hasEvents = eventCount > 0

          return (
            <div key={index} className="aspect-square">
              {day === null ? (
                <div></div>
              ) : (
                <button
                  onClick={() => handleDateClick(day)}
                  className={`w-full h-full rounded-lg text-xs font-medium transition-all flex flex-col items-center justify-center relative ${
                    isToday
                      ? 'bg-emerald-600 text-white'
                      : hasEvents
                        ? 'bg-emerald-50 text-emerald-900 border border-emerald-200 hover:bg-emerald-100'
                        : 'bg-gray-50 text-gray-600 hover:bg-gray-100'
                  }`}
                >
                  <span>{day}</span>
                  {hasEvents && (
                    <span
                      className={`text-xs font-bold mt-0.5 ${
                        isToday ? 'bg-emerald-800 text-white' : 'bg-emerald-200 text-emerald-900'
                      } rounded-full w-4 h-4 flex items-center justify-center`}
                    >
                      {eventCount > 9 ? '9+' : eventCount}
                    </span>
                  )}
                </button>
              )}
            </div>
          )
        })}
      </div>

      <div className="mt-4 pt-4 border-t border-gray-100">
        <div className="text-xs text-gray-600 space-y-1.5">
          <div className="flex items-center gap-2">
            <div className="w-3 h-3 rounded-full bg-emerald-600"></div>
            <span>Today</span>
          </div>
          <div className="flex items-center gap-2">
            <div className="w-3 h-3 rounded-full bg-emerald-100 border border-emerald-200"></div>
            <span>With events</span>
          </div>
          <div className="flex items-center gap-2">
            <div className="w-3 h-3 rounded-full bg-gray-100"></div>
            <span>No events</span>
          </div>
        </div>
      </div>
    </div>
  )
}
