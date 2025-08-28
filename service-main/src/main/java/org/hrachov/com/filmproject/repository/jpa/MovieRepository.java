package org.hrachov.com.filmproject.repository.jpa;

import org.hrachov.com.filmproject.model.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> , JpaSpecificationExecutor<Movie> {
    List<Movie> findByTitle(String title);

    List<Movie> findByTitleIgnoreCase(String title);
}
