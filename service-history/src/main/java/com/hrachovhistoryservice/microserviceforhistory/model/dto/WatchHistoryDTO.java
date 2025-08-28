package com.hrachovhistoryservice.microserviceforhistory.model.dto;

import com.hrachovhistoryservice.microserviceforhistory.model.Film;
import com.hrachovhistoryservice.microserviceforhistory.model.User;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class WatchHistoryDTO{
    private Long id;
    private Long userId;
    private Long filmId;
    private LocalDateTime date;
}
