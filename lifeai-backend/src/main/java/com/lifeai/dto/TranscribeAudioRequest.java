package com.lifeai.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranscribeAudioRequest {

    @NotNull(message = "Audio file is required")
    private MultipartFile audioFile;

    private String language;

    private String model;
}
