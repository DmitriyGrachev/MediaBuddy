package com.hrachovcompressionservice.microserviceforvideocompression.config;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

// Можно также объявить бины для RabbitTemplate, если он нужен для чего-то еще,
// но для простого слушателя он не обязателен в конфигурации.
@Configuration
@EnableRabbit// Важно для работы @RabbitListener
@EnableScheduling
@EnableAsync
public class MainConfig {
}
