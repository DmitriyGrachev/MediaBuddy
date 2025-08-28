package org.hrachov.com.filmproject.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.antlr.v4.runtime.misc.NotNull;
import org.hrachov.com.filmproject.model.Comment;
import org.hrachov.com.filmproject.service.CommentService;
import org.hrachov.com.filmproject.service.impl.CommentServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentDTO {
    private Long id;
    private Long filmId;
    private String text;
    private LocalDateTime time;
    private long likes;
    private long dislikes;
    private String username; // Make sure this matches what you set (user.getUsername())
    private List<CommentDTO> replies = Collections.emptyList(); // Initialize to avoid nulls
    private Long parentId; // Optional: useful for client-side logic if needed

    // You might want a constructor without replies for simplicity in some cases
}