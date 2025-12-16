package com.lifeai.controller;

import com.lifeai.dto.TranscriptionResponse;
import com.lifeai.service.SpeechToTextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/speech-to-text")
@RequiredArgsConstructor
public class SpeechToTextController {

    private final SpeechToTextService speechToTextService;

    @PostMapping("/transcribe")
    public ResponseEntity<TranscriptionResponse> transcribeAudio(
            @RequestParam(value = "file") MultipartFile audioFile,
            @RequestParam(value = "language", required = false) String language) {

        long startTime = System.currentTimeMillis();
        
        log.info("Transcribing audio file: {}", audioFile.getOriginalFilename());

        String transcribedText = speechToTextService.transcribeAudio(audioFile);

        long processingTime = System.currentTimeMillis() - startTime;

        TranscriptionResponse response = TranscriptionResponse.builder()
                .text(transcribedText)
                .sourceFile(audioFile.getOriginalFilename())
                .language(language)
                .processingTimeMs(processingTime)
                .build();

        log.info("Audio transcription completed in {}ms", processingTime);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/transcribe-bytes")
    public ResponseEntity<TranscriptionResponse> transcribeAudioBytes(
            @RequestBody byte[] audioBytes,
            @RequestParam(value = "mimeType") String mimeType,
            @RequestParam(value = "language", required = false) String language) {

        long startTime = System.currentTimeMillis();

        log.info("Transcribing audio bytes with mime type: {}", mimeType);

        String transcribedText = speechToTextService.transcribeAudio(audioBytes, mimeType);

        long processingTime = System.currentTimeMillis() - startTime;

        TranscriptionResponse response = TranscriptionResponse.builder()
                .text(transcribedText)
                .language(language)
                .processingTimeMs(processingTime)
                .build();

        log.info("Audio transcription completed in {}ms", processingTime);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
