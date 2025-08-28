package org.hrachov.com.filmproject.model.dto;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hrachov.com.filmproject.model.youtube.VideoFrame;

import java.util.List;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoDirectoryDTO {
    private int id;
    private List<VideoFrameDTO> videoFrames;
    private String description;
}