package org.hrachov.com.filmproject.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class CompressionRequest {
    private String videoId;
    private String absoluteFilePath;
    private String username;
    private String replyTo;
    private String correlationId;
    private Integer retries;
    private List<String> qualities;
}
