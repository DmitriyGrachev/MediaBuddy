package org.hrachov.com.filmproject.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.hrachov.com.filmproject.model.dto.InteractionDTO;
import org.hrachov.com.filmproject.model.dto.WatchEventDto;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.rabbit.stream.producer.RabbitStreamTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class HistoryEventSender {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitStreamTemplate rabbitStreamTemplate;
    private final ObjectMapper objectMapper;

    @SneakyThrows
    public void sendWatchEvent(Long userId, Long filmId, String type, Double value) {
        rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());

        rabbitTemplate.convertAndSend(
                "history.exchange",
                "history.routing.key",
                new WatchEventDto(userId, filmId, 0.0),
                message -> {
                    message.getMessageProperties().setContentType("application/json");
                    return message;
                }
        );
        log.info("Sending watch event");

        InteractionDTO userEventDto = InteractionDTO.builder()
                .userId(userId.toString()).filmId(filmId.toString()).timestamp(LocalDateTime.now()).interactionType(type).value(value).build();

// Сериализуем вручную
        byte[] payload = objectMapper.writeValueAsBytes(userEventDto);

        rabbitStreamTemplate.convertAndSend(payload);

    }
}
