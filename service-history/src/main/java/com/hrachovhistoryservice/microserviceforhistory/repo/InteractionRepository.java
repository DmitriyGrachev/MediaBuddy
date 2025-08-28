package com.hrachovhistoryservice.microserviceforhistory.repo;

import com.hrachovhistoryservice.microserviceforhistory.model.Interaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InteractionRepository extends JpaRepository<Interaction, Integer> {
    List<Interaction> findByUserId(String userId);
}
