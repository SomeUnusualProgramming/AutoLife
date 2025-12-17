interface SkeletonLoaderProps {
  type?: 'timeline' | 'recommendations'
  count?: number
}

const TimelineSkeletonCard = () => (
  <div className="space-y-3 mb-6">
    <div className="flex items-center gap-3 mb-4 pb-3 border-b border-gray-100">
      <div className="h-5 w-32 bg-gray-200 rounded animate-pulse"></div>
      <div className="h-5 w-12 bg-gray-200 rounded-full animate-pulse"></div>
    </div>

    <div className="space-y-4">
      {[1, 2].map((periodIndex) => (
        <div key={periodIndex} className="space-y-3">
          <div className="flex items-center gap-2">
            <div className="h-5 w-16 bg-gray-200 rounded animate-pulse"></div>
          </div>

          <div className="relative space-y-3 ml-8">
            {[1, 2].map((entryIndex) => (
              <div key={entryIndex} className="relative">
                <div className="flex gap-3">
                  <div className="flex-shrink-0 relative z-10 mt-0.5">
                    <div className="h-3 w-3 rounded-full bg-gray-200 animate-pulse"></div>
                  </div>

                  <div className="flex-1 min-w-0">
                    <div className="rounded-lg p-3.5 border border-gray-200 bg-gray-50">
                      <div className="space-y-2">
                        <div className="h-4 w-3/4 bg-gray-200 rounded animate-pulse"></div>
                        <div className="h-3 w-1/2 bg-gray-200 rounded animate-pulse"></div>
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
  </div>
)

const RecommendationSkeletonCard = () => (
  <div className="p-3.5 rounded-lg border border-gray-200 bg-gray-50">
    <div className="space-y-2">
      <div className="h-4 w-5/6 bg-gray-200 rounded animate-pulse"></div>
      <div className="h-3 w-2/3 bg-gray-200 rounded animate-pulse"></div>
      <div className="flex gap-2 pt-2">
        <div className="h-5 w-16 bg-gray-200 rounded-full animate-pulse"></div>
        <div className="h-5 w-12 bg-gray-200 rounded-full animate-pulse"></div>
      </div>
    </div>
  </div>
)

export const SkeletonLoader: React.FC<SkeletonLoaderProps> = ({ type = 'timeline', count = 3 }) => {
  if (type === 'recommendations') {
    return (
      <div className="space-y-3">
        {Array.from({ length: count }).map((_, index) => (
          <RecommendationSkeletonCard key={index} />
        ))}
      </div>
    )
  }

  return (
    <div className="space-y-6">
      {Array.from({ length: count }).map((_, index) => (
        <TimelineSkeletonCard key={index} />
      ))}
    </div>
  )
}
