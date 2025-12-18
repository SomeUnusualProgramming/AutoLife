import { useState } from 'react'
import { Event } from '../types'

interface IncompleteEventCardProps {
  event: Event
  onEdit?: (event: Event) => void
}

export const IncompleteEventCard = ({ event, onEdit }: IncompleteEventCardProps) => {
  const [isExpanded, setIsExpanded] = useState(false)

  const getMissingFields = () => {
    const missing = []
    const metadata = event.metadata || {}

    if (!metadata.description) missing.push('Opis')
    if (!metadata.duration_minutes) missing.push('Czas trwania')
    if (!metadata.intensity) missing.push('Intensywność')
    if (!metadata.dosage) missing.push('Dawka')
    if (!metadata.medication_name) missing.push('Nazwa leku')
    if (!metadata.doctor_name) missing.push('Lekarz')

    return missing.length > 0 ? missing : ['Informacje szczegółowe']
  }

  const missingFields = getMissingFields()

  return (
    <div className="mb-4 border-l-4 border-yellow-500 bg-yellow-50 p-4 rounded-lg">
      <div className="flex items-start justify-between">
        <div className="flex-1">
          <div className="flex items-center gap-2 mb-2">
            <span className="px-2 py-1 bg-yellow-200 text-yellow-800 text-xs font-semibold rounded">
              ⚠ NIEKOMPLETNE
            </span>
            {event.type && (
              <span className="text-xs text-gray-600">
                {event.type}
              </span>
            )}
          </div>

          <p className="font-medium text-gray-800 mb-1">
            {event.description || 'Brak opisu'}
          </p>

          {event.timestamp && (
            <p className="text-sm text-gray-600 mb-2">
              {new Date(event.timestamp).toLocaleString('pl-PL')}
            </p>
          )}

          {!isExpanded && (
            <div className="text-sm text-yellow-700 mb-2">
              Brakuje informacji: {missingFields.slice(0, 2).join(', ')}
              {missingFields.length > 2 && ` +${missingFields.length - 2} więcej`}
            </div>
          )}

          {isExpanded && (
            <div className="mt-3 bg-white rounded p-3 border border-yellow-200">
              <p className="text-sm font-semibold text-gray-700 mb-2">Brakujące pola:</p>
              <ul className="text-sm text-gray-600 space-y-1">
                {missingFields.map((field) => (
                  <li key={field} className="flex items-center">
                    <span className="text-yellow-500 mr-2">•</span>
                    {field}
                  </li>
                ))}
              </ul>

              {event.metadata && Object.keys(event.metadata).length > 0 && (
                <div className="mt-3 pt-3 border-t border-yellow-200">
                  <p className="text-sm font-semibold text-gray-700 mb-2">Dostępne dane:</p>
                  <div className="space-y-1">
                    {Object.entries(event.metadata)
                      .filter(([, value]) => value)
                      .map(([key, value]) => (
                        <p key={key} className="text-xs text-gray-600">
                          <span className="font-medium">{key}:</span> {String(value)}
                        </p>
                      ))}
                  </div>
                </div>
              )}

              <p className="text-xs text-gray-500 mt-3 italic">
                Event został automatycznie zapisany po 3 rundach wyjaśniania.
                Możesz edytować go teraz, aby dodać brakujące informacje.
              </p>
            </div>
          )}
        </div>

        <div className="flex flex-col gap-2 ml-4">
          <button
            onClick={() => setIsExpanded(!isExpanded)}
            className="px-3 py-2 text-sm text-yellow-700 hover:bg-yellow-100 rounded transition"
          >
            {isExpanded ? 'Schowaj' : 'Szczegóły'}
          </button>

          {onEdit && (
            <button
              onClick={() => onEdit(event)}
              className="px-3 py-2 text-sm bg-yellow-500 text-white rounded hover:bg-yellow-600 transition font-medium"
            >
              Edytuj
            </button>
          )}
        </div>
      </div>
    </div>
  )
}
