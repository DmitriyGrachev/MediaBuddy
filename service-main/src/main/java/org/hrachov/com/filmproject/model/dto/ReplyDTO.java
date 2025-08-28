package org.hrachov.com.filmproject.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReplyDTO {
    private String text;
    // filmId is not strictly needed as it can be derived from the parent comment
}
