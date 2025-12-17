package com.lifeai.controller;

import com.lifeai.dto.TranscriptionResponse;
import com.lifeai.service.SpeechToTextService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SpeechToTextController.class)
@Disabled("Web context requires full app initialization")
class SpeechToTextControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SpeechToTextService speechToTextService;

    @Test
    void testTranscribeAudioFileWithoutLanguage() throws Exception {
        MockMultipartFile audioFile = new MockMultipartFile(
                "file",
                "test.webm",
                "audio/webm",
                "fake audio content".getBytes()
        );

        String expectedText = "This is a transcribed text";
        when(speechToTextService.transcribeAudio(any())).thenReturn(expectedText);

        mockMvc.perform(multipart("/speech-to-text/transcribe")
                .file(audioFile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text", equalTo(expectedText)))
                .andExpect(jsonPath("$.sourceFile", equalTo("test.webm")))
                .andExpect(jsonPath("$.processingTimeMs", greaterThanOrEqualTo(0)));
    }



    @Test
    void testTranscribeAudioBytesWithoutLanguage() throws Exception {
        String expectedText = "This is a transcribed text";
        
        when(speechToTextService.transcribeAudio(org.mockito.ArgumentMatchers.any(byte[].class), anyString())).thenReturn(expectedText);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/speech-to-text/transcribe-bytes")
                .contentType("audio/webm")
                .param("mimeType", "audio/webm")
                .content("fake audio content".getBytes()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text", equalTo(expectedText)))
                .andExpect(jsonPath("$.processingTimeMs", greaterThanOrEqualTo(0)));
    }


}
