package com.hrachovcompressionservice.microserviceforvideocompression.model;

import com.hrachovcompressionservice.microserviceforvideocompression.model.dto.VideoStatus;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.Map;
import java.util.Set;

@Getter
@Setter
@Document(collection = "videos") // Назовем коллекцию проще
public class CustomVideo {

    @Id
    private String id;
    private String userId;
    private String title;
    private String description;

    private VideoStatus status; // Enum: UPLOADING, PROCESSING, COMPLETED, FAILED

    // Гибкая структура для файлов
    private Map<String, Object> originalFile;   // Информация об оригинальном файле
    private Map<String, Object> processedFiles; // Здесь будут храниться результаты компрессии
    /*
      Пример для processedFiles:
      "processedFiles": {
          "720p": { "path": "/path/to/video_720.mp4", "size": 123456 },
          "480p": { "path": "/path/to/video_480.mp4", "size": 78910 }
      }
    */

    // Гибкая структура для метаданных
    private Map<String, Object> metadataOriginal; // Сюда сложим все, что извлечем из видео
    /*
      Пример для metadata:
      "metadata": {
          "duration_sec": 125,
          "resolution": "1920x1080",
          "format": "mov"
      }
    */
    private Map<String, Object> metadataProcessed;

    private String thumbnailPath; // Путь к одному сгенерированному thumbnail
    private Date createdAt;
    private String errorMessage; // На случай ошибки
    private Set<String> tags;

}