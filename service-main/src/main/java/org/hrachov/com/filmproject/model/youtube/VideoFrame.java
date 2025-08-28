package org.hrachov.com.filmproject.model.youtube;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.IdentityHashMap;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoFrame {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_directory_id")
    @JsonBackReference // Предотвращает сериализацию videoDirectory
    private VideoDirectory videoDirectory;

    private String title;
    private String description;
    private String url;
}