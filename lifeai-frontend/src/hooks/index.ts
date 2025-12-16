import { useState, useCallback, useRef, useEffect } from 'react'
import { Event, Timeline, Recommendation, TranscriptionResult } from '../types'
import { eventApi, timelineApi, recommendationsApi, speechToTextApi } from '../services/api'

export const useAsync = <T,>(
  asyncFunction: () => Promise<T>,
  immediate = true
) => {
  const [status, setStatus] = useState<'idle' | 'pending' | 'success' | 'error'>('idle')
  const [data, setData] = useState<T | null>(null)
  const [error, setError] = useState<Error | null>(null)

  const execute = useCallback(async () => {
    setStatus('pending')
    setData(null)
    setError(null)

    try {
      const response = await asyncFunction()
      setData(response)
      setStatus('success')
      return response
    } catch (err) {
      setError(err instanceof Error ? err : new Error(String(err)))
      setStatus('error')
    }
  }, [asyncFunction])

  if (immediate) {
    execute()
  }

  return { execute, status, data, error }
}

export const useSendEvent = () => {
  const [status, setStatus] = useState<'idle' | 'pending' | 'success' | 'error'>('idle')
  const [data, setData] = useState<Event | null>(null)
  const [error, setError] = useState<Error | null>(null)

  const send = useCallback(async (event: Event) => {
    setStatus('pending')
    setData(null)
    setError(null)

    try {
      const response = await eventApi.sendEvent(event)
      if (response.data) {
        setData(response.data)
        setStatus('success')
        return response.data
      }
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to send event'
      setError(new Error(message))
      setStatus('error')
      throw err
    }
  }, [])

  return { send, status, data, error }
}

export const useTimeline = (limit?: number, offset?: number) => {
  const [status, setStatus] = useState<'idle' | 'pending' | 'success' | 'error'>('idle')
  const [data, setData] = useState<Timeline | null>(null)
  const [error, setError] = useState<Error | null>(null)

  const fetch = useCallback(async () => {
    setStatus('pending')
    setData(null)
    setError(null)

    try {
      const response = await timelineApi.getTimeline(limit, offset)
      if (response.data) {
        setData(response.data)
        setStatus('success')
        return response.data
      }
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to fetch timeline'
      setError(new Error(message))
      setStatus('error')
      throw err
    }
  }, [limit, offset])

  useEffect(() => {
    fetch()
  }, [fetch])

  return { fetch, status, data, error }
}

export const useRecommendations = (category?: string) => {
  const [status, setStatus] = useState<'idle' | 'pending' | 'success' | 'error'>('idle')
  const [data, setData] = useState<Recommendation[] | null>(null)
  const [error, setError] = useState<Error | null>(null)

  const fetch = useCallback(async () => {
    setStatus('pending')
    setData(null)
    setError(null)

    try {
      const response = await recommendationsApi.getRecommendations(category)
      if (response.data) {
        setData(response.data)
        setStatus('success')
        return response.data
      }
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to fetch recommendations'
      setError(new Error(message))
      setStatus('error')
      throw err
    }
  }, [category])

  const markDone = useCallback(async (id: string) => {
    try {
      const response = await recommendationsApi.markRecommendationDone(id)
      if (response.data && data) {
        const updated = data.map(rec => rec.id === id ? response.data : rec).filter((rec): rec is Recommendation => !!rec)
        setData(updated)
      }
      return response.data
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to mark recommendation as done'
      setError(new Error(message))
      throw err
    }
  }, [data])

  const markPlanned = useCallback(async (id: string) => {
    try {
      const response = await recommendationsApi.markRecommendationPlanned(id)
      if (response.data && data) {
        const updated = data.map(rec => rec.id === id ? response.data : rec).filter((rec): rec is Recommendation => !!rec)
        setData(updated)
      }
      return response.data
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to mark recommendation as planned'
      setError(new Error(message))
      throw err
    }
  }, [data])

  useEffect(() => {
    fetch()
  }, [fetch])

  return { fetch, status, data, error, markDone, markPlanned }
}

export const useAudioRecorder = () => {
  const [isRecording, setIsRecording] = useState(false)
  const [audioBlob, setAudioBlob] = useState<Blob | null>(null)
  const mediaRecorderRef = useRef<MediaRecorder | null>(null)
  const audioContextRef = useRef<AudioContext | null>(null)
  const streamRef = useRef<MediaStream | null>(null)

  const startRecording = useCallback(async () => {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
      streamRef.current = stream

      const AudioContextClass = window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext
      const audioContext = new AudioContextClass()
      audioContextRef.current = audioContext

      const mediaRecorder = new MediaRecorder(stream, {
        mimeType: 'audio/webm;codecs=opus',
      })
      mediaRecorderRef.current = mediaRecorder

      const chunks: BlobPart[] = []

      mediaRecorder.ondataavailable = (event) => {
        chunks.push(event.data)
      }

      mediaRecorder.onstop = () => {
        const blob = new Blob(chunks, { type: 'audio/webm' })
        setAudioBlob(blob)
      }

      mediaRecorder.start()
      setIsRecording(true)
    } catch (error) {
      console.error('Failed to start recording:', error)
      throw new Error('Unable to access microphone. Please check permissions.')
    }
  }, [])

  const stopRecording = useCallback(() => {
    if (mediaRecorderRef.current && isRecording) {
      mediaRecorderRef.current.stop()
      streamRef.current?.getTracks().forEach((track) => track.stop())
      audioContextRef.current?.close()
      setIsRecording(false)
    }
  }, [isRecording])

  const resetRecording = useCallback(() => {
    setAudioBlob(null)
  }, [])

  return {
    isRecording,
    audioBlob,
    startRecording,
    stopRecording,
    resetRecording,
  }
}

export const useSpeechToText = () => {
  const [status, setStatus] = useState<'idle' | 'pending' | 'success' | 'error'>('idle')
  const [transcribedText, setTranscribedText] = useState('')
  const [error, setError] = useState<Error | null>(null)

  const transcribe = useCallback(async (audioBlob: Blob, language?: string) => {
    setStatus('pending')
    setError(null)
    setTranscribedText('')

    try {
      const audioFile = new File([audioBlob], 'recording.webm', { type: 'audio/webm' })
      const result: TranscriptionResult = await speechToTextApi.transcribeAudio(audioFile, language)
      setTranscribedText(result.text)
      setStatus('success')
      return result
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to transcribe audio'
      setError(new Error(message))
      setStatus('error')
      throw err
    }
  }, [])

  const resetTranscription = useCallback(() => {
    setTranscribedText('')
    setError(null)
    setStatus('idle')
  }, [])

  return {
    status,
    transcribedText,
    error,
    transcribe,
    resetTranscription,
  }
}
