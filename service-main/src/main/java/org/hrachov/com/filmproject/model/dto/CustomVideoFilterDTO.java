package org.hrachov.com.filmproject.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class CustomVideoFilterDTO {
    private Set<String> tags;
    private String sortBy;
    private String direction;
    private String title;
}
