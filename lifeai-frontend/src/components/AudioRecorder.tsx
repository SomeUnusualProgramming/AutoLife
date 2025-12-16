import { useState, useEffect } from 'react'
import { useAudioRecorder, useSpeechToText } from '../hooks'

interface AudioRecorderProps {
  onTranscriptionComplete?: (text: string) => void
}

export const AudioRecorder = ({ onTranscriptionComplete }: AudioRecorderProps) => {
  const { isRecording, audioBlob, startRecording, stopRecording, resetRecording } = useAudioRecorder()
  const { status: transcriptionStatus, transcribedText, error: transcriptionError, transcribe } = useSpeechToText()
  const [isProcessing, setIsProcessing] = useState(false)

  useEffect(() => {
    if (audioBlob && !transcribedText && !isProcessing) {
      const autoTranscribe = async () => {
        setIsProcessing(true)
        try {
          await transcribe(audioBlob)
        } catch (error) {
          console.error('Auto-transcription failed:', error)
        } finally {
          setIsProcessing(false)
        }
      }
      autoTranscribe()
    }
  }, [audioBlob, transcribedText, isProcessing, transcribe])

  const handleStartRecording = async () => {
    try {
      await startRecording()
    } catch (error) {
      console.error('Failed to start recording:', error)
    }
  }

  const handleStopRecording = () => {
    stopRecording()
  }

  const handleTranscribe = async () => {
    if (!audioBlob) return

    setIsProcessing(true)
    try {
      await transcribe(audioBlob)
    } catch (error) {
      console.error('Transcription failed:', error)
    } finally {
      setIsProcessing(false)
    }
  }

  const handleUseTranscription = () => {
    if (transcribedText && onTranscriptionComplete) {
      onTranscriptionComplete(transcribedText)
      resetRecording()
    }
  }

  const handleDiscard = () => {
    resetRecording()
  }

  return (
    <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6 mb-6">
      <h3 className="text-lg font-semibold text-gray-900 mb-4 flex items-center gap-2">
        <span className="text-xl">🎤</span>
        Voice Recording
      </h3>

      {!audioBlob && !transcribedText && (
        <div className="space-y-4">
          <div className="flex gap-3">
            <button
              onClick={handleStartRecording}
              disabled={isRecording}
              className="flex-1 bg-emerald-600 hover:bg-emerald-700 disabled:bg-gray-400 text-white font-semibold py-3 px-4 rounded-lg transition-colors flex items-center justify-center gap-2"
            >
              <span className="text-lg">●</span>
              {isRecording ? 'Recording...' : 'Start Recording'}
            </button>
            {isRecording && (
              <button
                onClick={handleStopRecording}
                className="bg-gray-600 hover:bg-gray-700 text-white font-semibold py-3 px-6 rounded-lg transition-colors flex items-center gap-2"
              >
                <span className="text-lg">■</span>
                Stop
              </button>
            )}
          </div>

          {isRecording && (
            <div className="flex items-center gap-2 text-emerald-600">
              <span className="inline-block w-2 h-2 bg-emerald-600 rounded-full animate-pulse"></span>
              <span className="text-sm font-medium">Recording in progress...</span>
            </div>
          )}
        </div>
      )}

      {audioBlob && !transcribedText && (
        <div className="space-y-4">
          <div className="bg-emerald-50 border border-emerald-200 rounded-lg p-4">
            <p className="text-sm text-emerald-800">
              ✓ Recording ready ({(audioBlob.size / 1024).toFixed(2)} KB)
            </p>
          </div>

          {(isProcessing || transcriptionStatus === 'pending') ? (
            <div className="flex items-center gap-2">
              <div className="inline-block">
                <div className="animate-spin rounded-full h-5 w-5 border-2 border-emerald-200 border-t-emerald-600"></div>
              </div>
              <span className="text-sm text-emerald-700 font-medium">Transcribing...</span>
            </div>
          ) : (
            <div className="flex gap-3">
              <button
                onClick={handleTranscribe}
                disabled={isProcessing}
                className="flex-1 bg-emerald-600 hover:bg-emerald-700 disabled:bg-gray-400 text-white font-semibold py-3 px-4 rounded-lg transition-colors flex items-center justify-center gap-2"
              >
                <span className="text-lg">⚡</span>
                Transcribe
              </button>
              <button
                onClick={handleDiscard}
                disabled={isProcessing}
                className="bg-gray-500 hover:bg-gray-600 disabled:bg-gray-400 text-white font-semibold py-3 px-6 rounded-lg transition-colors"
              >
                Delete
              </button>
            </div>
          )}
        </div>
      )}

      {transcribedText && (
        <div className="space-y-4">
          <div className="bg-emerald-50 border border-emerald-200 rounded-lg p-4">
            <p className="text-sm font-medium text-emerald-800 mb-2">✓ Transcription complete</p>
            <p className="text-gray-800 text-sm leading-relaxed break-words">{transcribedText}</p>
          </div>

          <div className="flex gap-3">
            <button
              onClick={handleUseTranscription}
              className="flex-1 bg-emerald-600 hover:bg-emerald-700 text-white font-semibold py-3 px-4 rounded-lg transition-colors flex items-center justify-center gap-2"
            >
              <span className="text-lg">✓</span>
              Use Text
            </button>
            <button
              onClick={handleDiscard}
              className="bg-gray-500 hover:bg-gray-600 text-white font-semibold py-3 px-6 rounded-lg transition-colors"
            >
              Cancel
            </button>
          </div>
        </div>
      )}

      {transcriptionError && (
        <div className="bg-rose-50 border border-rose-200 rounded-lg p-4 mt-4">
          <p className="text-sm text-rose-700">
            <span className="font-medium">Error:</span> {transcriptionError.message}
          </p>
        </div>
      )}
    </div>
  )
}
