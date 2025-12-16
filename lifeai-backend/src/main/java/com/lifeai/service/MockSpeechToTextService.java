package com.lifeai.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class MockSpeechToTextService implements SpeechToTextService {

    @Override
    public String transcribeAudio(MultipartFile audioFile) {
        log.info("Mock transcribing audio file: {}", audioFile.getOriginalFilename());
        return generateMockTranscription(audioFile.getOriginalFilename());
    }

    @Override
    public String transcribeAudio(byte[] audioBytes, String mimeType) {
        log.info("Mock transcribing audio bytes with mime type: {}", mimeType);
        return generateMockTranscription("audio_bytes");
    }

    private String generateMockTranscription(String sourceInfo) {
        return "This is a mock transcription for: " + sourceInfo + ". " +
               "In production, this would contain the actual speech-to-text recognition result.";
    }
}
