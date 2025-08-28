package org.hrachov.com.filmproject.model.dto;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hrachov.com.filmproject.model.youtube.VideoDirectory;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoFrameDTO {
    private long id;
    private Long videoDirectoryId; // Заменяем VideoDirectory на ID
    private String title;
    private String description;
    private String url;
}