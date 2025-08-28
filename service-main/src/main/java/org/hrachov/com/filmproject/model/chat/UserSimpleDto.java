package org.hrachov.com.filmproject.model.chat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data // Lombok аннотация для геттеров, сеттеров, toString и т.д.
@NoArgsConstructor
@AllArgsConstructor
public class UserSimpleDto {
    private Long id;
    private String username;
    private String firstName;
    private String lastName;
}
