package com.lifeai.service;

import org.springframework.web.multipart.MultipartFile;

public interface SpeechToTextService {

    String transcribeAudio(MultipartFile audioFile);

    String transcribeAudio(byte[] audioBytes, String mimeType);
}
