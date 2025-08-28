package org.hrachov.com.filmproject.model.dto;

import jakarta.persistence.*;
import lombok.Data;
import org.hrachov.com.filmproject.model.Comment;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserDTO {
    public Long id;
    public String username;
    public String email;
    public String firstName;
    public String lastName;
    public LocalDateTime registrationDate;
}
