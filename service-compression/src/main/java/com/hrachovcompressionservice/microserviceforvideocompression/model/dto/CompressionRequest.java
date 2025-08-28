package com.hrachovcompressionservice.microserviceforvideocompression.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CompressionRequest {
    private String videoId;
    private String absoluteFilePath;
    private String username;
    private String replyTo;
    private String correlationId;
    private Integer retries;
    private List<String> qualities;
}
