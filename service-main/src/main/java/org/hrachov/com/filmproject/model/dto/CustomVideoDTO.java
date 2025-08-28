package org.hrachov.com.filmproject.model.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.hrachov.com.filmproject.model.VideoStatus;

import java.util.Map;
import java.util.Set;

@Getter
@Setter
@Builder
public class CustomVideoDTO {
    private String title;
    private String description;
    private VideoStatus status;
    private String thumbnailUrl;
    // мапа: качество → presigned URL
    private Map<String, String> videoUrls;
    // мапа: качество → метаданные
    private Map<String,  Object> metadata;
    private Set<String> tags;
}
