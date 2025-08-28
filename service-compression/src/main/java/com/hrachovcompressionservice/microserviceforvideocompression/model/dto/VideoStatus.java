package com.hrachovcompressionservice.microserviceforvideocompression.model.dto;

public enum VideoStatus {
    UPLOADING,
    PROCESSING,
    POSTMETADATA,
    THUMBNAIL,
    COMPLETED,
    FAILED
}