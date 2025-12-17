package com.lifeai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WhisperSpeechToTextServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private WhisperSpeechToTextService whisperService;
    private static final String TEST_API_KEY = "test-api-key";
    private static final String DEFAULT_LANGUAGE = "pl";

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        whisperService = new WhisperSpeechToTextService(restTemplate, TEST_API_KEY, DEFAULT_LANGUAGE, objectMapper);
    }

    @Test
    void testTranscribeAudioFileWithDefaultLanguage() {
        MockMultipartFile audioFile = new MockMultipartFile(
                "file",
                "test.webm",
                "audio/webm",
                "fake audio content".getBytes()
        );

        String expectedText = "To jest test transkrypcji";
        String mockResponse = "{\"text\": \"" + expectedText + "\"}";

        when(restTemplate.postForObject(
                anyString(),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(mockResponse);

        String result = whisperService.transcribeAudio(audioFile);

        assertEquals(expectedText, result);
    }

    @Test
    void testTranscribeAudioFileWithSpecificLanguage() {
        MockMultipartFile audioFile = new MockMultipartFile(
                "file",
                "test.webm",
                "audio/webm",
                "fake audio content".getBytes()
        );

        String expectedText = "This is a test transcription";
        String mockResponse = "{\"text\": \"" + expectedText + "\"}";

        when(restTemplate.postForObject(
                anyString(),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(mockResponse);

        String result = whisperService.transcribeAudio(audioFile, "en");

        assertEquals(expectedText, result);
    }

    @Test
    void testTranscribeAudioBytesWithDefaultLanguage() {
        byte[] audioBytes = "fake audio content".getBytes();
        String expectedText = "To jest test transkrypcji";
        String mockResponse = "{\"text\": \"" + expectedText + "\"}";

        when(restTemplate.postForObject(
                anyString(),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(mockResponse);

        String result = whisperService.transcribeAudio(audioBytes, "audio/webm");

        assertEquals(expectedText, result);
    }

    @Test
    void testTranscribeAudioBytesWithSpecificLanguage() {
        byte[] audioBytes = "fake audio content".getBytes();
        String expectedText = "This is a test transcription";
        String mockResponse = "{\"text\": \"" + expectedText + "\"}";

        when(restTemplate.postForObject(
                anyString(),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(mockResponse);

        String result = whisperService.transcribeAudio(audioBytes, "audio/webm", "en");

        assertEquals(expectedText, result);
    }

    @Test
    void testTranscribeAudioPreservesPolishCharacters() {
        MockMultipartFile audioFile = new MockMultipartFile(
                "file",
                "test.webm",
                "audio/webm",
                "fake audio content".getBytes()
        );

        String expectedText = "Ąęóęśćźż łóąęśćźż";
        String mockResponse = "{\"text\": \"" + expectedText + "\"}";

        when(restTemplate.postForObject(
                anyString(),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(mockResponse);

        String result = whisperService.transcribeAudio(audioFile, "pl");

        assertEquals(expectedText, result);
    }

    @Test
    void testTranscribeAudioHandlesJsonResponse() {
        byte[] audioBytes = "fake audio content".getBytes();
        String expectedText = "Complex transcription with punctuation!";
        String mockResponse = "{\"text\": \"" + expectedText + "\", \"language\": \"pl\", \"duration\": 5.5}";

        when(restTemplate.postForObject(
                anyString(),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(mockResponse);

        String result = whisperService.transcribeAudio(audioBytes, "audio/webm", "pl");

        assertEquals(expectedText, result);
    }

    @Test
    void testTranscribeAudioThrowsExceptionOnInvalidResponse() {
        MockMultipartFile audioFile = new MockMultipartFile(
                "file",
                "test.webm",
                "audio/webm",
                "fake audio content".getBytes()
        );

        String invalidResponse = "invalid json";

        when(restTemplate.postForObject(
                anyString(),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(invalidResponse);

        assertThrows(RuntimeException.class, () -> whisperService.transcribeAudio(audioFile, "pl"));
    }

    @Test
    void testTranscribeAudioThrowsExceptionOnApiError() {
        MockMultipartFile audioFile = new MockMultipartFile(
                "file",
                "test.webm",
                "audio/webm",
                "fake audio content".getBytes()
        );

        when(restTemplate.postForObject(
                anyString(),
                any(HttpEntity.class),
                eq(String.class)
        )).thenThrow(new RuntimeException("API Error"));

        assertThrows(RuntimeException.class, () -> whisperService.transcribeAudio(audioFile, "pl"));
    }
}
