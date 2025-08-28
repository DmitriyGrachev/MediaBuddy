package org.hrachov.com.filmproject.model;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
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
    @CreationTimestamp
    private Date createdAt;
    private String errorMessage; // На случай ошибки
    private Set<String> tags;
}

/*
{
  "_id": ObjectId, // Unique identifier for the video
  "userId": String, // ID of the user who uploaded the video
  "title": String, // Optional video title provided by the user
  "description": String, // Optional description
  "originalFile": {
    "fileName": String, // Original file name (e.g., "myvideo.mp4")
    "fileSize": NumberLong, // Size in bytes
    "mimeType": String, // MIME type (e.g., "video/mp4")
    "uploadDate": ISODate, // Upload timestamp
    "storagePath": String // Path to original file (e.g., S3 URL or local path)
  },
  "compressedFile": {
    "fileName": String, // Compressed file name
    "fileSize": NumberLong, // Size in bytes
    "mimeType": String, // MIME type
    "storagePath": String, // Path to compressed file
    "compressionStatus": String, // Enum: "PENDING", "PROCESSING", "COMPLETED", "FAILED"
    "compressionError": String // Error message if compression fails
  },
  "thumbnail": {
    "fileName": String, // Thumbnail file name (e.g., "thumbnail.jpg")
    "fileSize": NumberLong, // Size in bytes
    "mimeType": String, // MIME type (e.g., "image/jpeg")
    "storagePath": String, // Path to thumbnail
    "generationStatus": String // Enum: "PENDING", "PROCESSING", "COMPLETED", "FAILED"
  },
  "status": String, // Overall video processing status: "UPLOADED", "CHECKING", "COMPRESSING", "THUMBNAIL_GENERATING", "COMPLETED", "FAILED"
  "errorDetails": String, // Details if error checking fails
  "duration": Number, // Video duration in seconds (extracted during processing)
  "resolution": String, // Video resolution (e.g., "1920x1080")
  "createdAt": ISODate, // Record creation timestamp
  "updatedAt": ISODate // Last update timestamp
}
 */