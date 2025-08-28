package org.hrachov.com.filmproject.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class GenreDTO {
    private Long id;
    private String name;
    // Constructors
    public GenreDTO() {}
    public GenreDTO(String name){
        this.name = name;
    }
    public GenreDTO(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}