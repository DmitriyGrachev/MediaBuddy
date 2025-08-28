package com.hrachovhistoryservice.microserviceforhistory.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WatchEventDto{
    private Long userId;
    private Long filmId;
    private double position; // lastPositionInSeconds
}
