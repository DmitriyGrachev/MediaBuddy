package com.hrachovcompressionservice.microserviceforvideocompression.model.dto;

import lombok.*;

import java.nio.file.Path;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompressionResponse {
    private String videoId; // ID документа в MongoDB
    private String username;
    private String absoluteFilePath; // Путь к ОРИГИНАЛЬНОМУ файлу
    private String correlationId;
    private String replyTo;

    // Эти поля можно будет заполнить по ходу дела
    private Map<String,Object> compressedFiles;
    private String videoPosterUrl;
}
