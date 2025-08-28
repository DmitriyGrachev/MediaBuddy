package com.hrachovhistoryservice.microserviceforhistory.repo;

import com.hrachovhistoryservice.microserviceforhistory.model.User;
import com.hrachovhistoryservice.microserviceforhistory.model.WatchHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WatchHistoryRepository  extends JpaRepository<WatchHistory, Integer> {
    List<WatchHistory> findByUserIdOrderByDateDesc(Long userId);

    WatchHistory findByUserIdAndFilmId(Long userId, Long filmId);
}
