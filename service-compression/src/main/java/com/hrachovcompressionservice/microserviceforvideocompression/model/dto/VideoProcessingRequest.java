package com.hrachovcompressionservice.microserviceforvideocompression.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class VideoProcessingRequest {
    private String taskId;
    private String userId;
    private String inputUrl; // ссылка на исходный файл в облаке
    private String outputUrl; // куда сохранять после обработки
    private String callbackUrl; // куда сообщить о завершении
}
