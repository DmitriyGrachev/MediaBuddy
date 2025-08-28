package org.hrachov.com.filmproject.model.dto;

import lombok.*;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SearchRequestDto {
    public String title;
    public Integer year;
    public String genre;

    public String sortDir;
    public String sortBy;
}
