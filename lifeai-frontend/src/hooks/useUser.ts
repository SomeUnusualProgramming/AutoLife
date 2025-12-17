import { useState, useEffect, useCallback } from 'react'

const DEFAULT_USER_ID = 1
const USER_ID_KEY = 'lifeai_user_id'

export const useUser = () => {
  const [userId, setUserId] = useState<number | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    const initializeUser = () => {
      try {
        const storedUserId = localStorage.getItem(USER_ID_KEY)
        
        if (storedUserId) {
          setUserId(parseInt(storedUserId, 10))
        } else {
          setUserId(DEFAULT_USER_ID)
          localStorage.setItem(USER_ID_KEY, String(DEFAULT_USER_ID))
        }
      } catch (error) {
        console.error('Failed to initialize user:', error)
        setUserId(DEFAULT_USER_ID)
      } finally {
        setIsLoading(false)
      }
    }

    initializeUser()
  }, [])

  const updateUserId = useCallback((newUserId: number) => {
    try {
      localStorage.setItem(USER_ID_KEY, String(newUserId))
      setUserId(newUserId)
    } catch (error) {
      console.error('Failed to update userId:', error)
    }
  }, [])

  return { userId, isLoading, updateUserId }
}
