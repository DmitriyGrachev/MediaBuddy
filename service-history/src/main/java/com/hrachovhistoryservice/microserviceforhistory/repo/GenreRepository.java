package com.hrachovhistoryservice.microserviceforhistory.repo;

import com.hrachovhistoryservice.microserviceforhistory.model.Genre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GenreRepository extends JpaRepository<Genre, Integer> {

}
