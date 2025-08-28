package org.hrachov.com.filmproject.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.repository.cdi.Eager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "comments")
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference("user-comments") // То же имя, что и в @JsonManagedReference
    private User user;

    @ManyToOne
    @JoinColumn(name = "film_id", nullable = false)
    private Film film;

    @Column(columnDefinition = "TEXT")
    private String text;

    @CreationTimestamp
    @Column(name = "time")
    private LocalDateTime time;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    @JsonBackReference // Указывает, что это "обратная" часть связи, не сериализуется полностью
    private Comment parentComment;

    @OneToMany(mappedBy = "parentComment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference // Указывает, что это "управляющая" часть связи, будет сериализована
    // @OrderBy("time ASC") // Опционально
    private List<Comment> replies = new ArrayList<>();

    // Конструкторы и хелперы остаются без изменений
    public Comment(User user, Film film, String text) {
        this.user = user;
        this.film = film;
        this.text = text;
    }

    public Comment(User user, Film film, String text, Comment parentComment) {
        this.user = user;
        this.film = film;
        this.text = text;
        this.parentComment = parentComment;
    }

    public void addReply(Comment reply) {
        this.replies.add(reply);
        reply.setParentComment(this);
    }

    public void removeReply(Comment reply) {
        this.replies.remove(reply);
        reply.setParentComment(null);
    }
}
