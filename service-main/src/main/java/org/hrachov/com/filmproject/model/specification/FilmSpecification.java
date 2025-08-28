package org.hrachov.com.filmproject.model.specification;

import org.hrachov.com.filmproject.model.Film;
import org.hrachov.com.filmproject.model.Movie;
import org.springframework.data.jpa.domain.Specification;

public class FilmSpecification {
    public static Specification<Film> getAllFilmsNew(){
        return (root,query,cb) ->{
            query.orderBy(cb.desc(root.get("releaseYear")));

            return cb.conjunction();
        };
    }
}