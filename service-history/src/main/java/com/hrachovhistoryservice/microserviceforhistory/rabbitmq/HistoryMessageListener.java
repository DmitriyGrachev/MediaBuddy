package com.hrachovhistoryservice.microserviceforhistory.rabbitmq;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.hrachovhistoryservice.microserviceforhistory.model.Interaction;
import com.hrachovhistoryservice.microserviceforhistory.model.WatchHistory;
import com.hrachovhistoryservice.microserviceforhistory.model.dto.InteractionDTO;

import com.hrachovhistoryservice.microserviceforhistory.repo.InteractionRepository;
import com.hrachovhistoryservice.microserviceforhistory.service.WatchHistoryService;
import com.rabbitmq.stream.impl.MessageBatch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import javax.security.auth.callback.ConfirmationCallback;
import java.io.IOException;

import java.time.LocalDateTime;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class HistoryMessageListener {

    private final WatchHistoryService watchHistoryService;
    private final ObjectMapper objectMapper;
    private final InteractionRepository interactionRepository;

    @RabbitListener(id = "stream-consumer-history_save", queues = "history.stream", containerFactory = "nativeFactory")
    public void receiveHistoryEvent(@Payload byte[] messageBody) throws IOException{
        // event содержит userId, filmId, возможно позицию просмотра
        try{
            InteractionDTO watchEventDto = objectMapper.readValue(messageBody, InteractionDTO.class);
            WatchHistory watchHistory = watchHistoryService.getWatchHistoryById(Long.valueOf(watchEventDto.getUserId()),
                    Long.valueOf(watchEventDto.getFilmId()));

            if(watchHistory == null){
                WatchHistory watchHistorySaved = watchHistoryService.saveWatchHistoryByIds(Long.valueOf(watchEventDto.getUserId()),
                        Long.valueOf(watchEventDto.getFilmId()),
                        watchEventDto.getInteractionType(),
                        watchEventDto.getValue());

                log.info("Received watch event {}", watchHistorySaved.toString());
                return;
            }
            watchHistory.setDate(LocalDateTime.now());
            log.info("Watch history {}", watchHistory.toString());
            watchHistoryService.getWatchHistoryById(watchHistory.getUserId(), watchHistory.getFilmId());

        }catch (Exception e){
            e.printStackTrace();
        }

    }

    // Stream listener - handle raw bytes
    @RabbitListener(id = "stream-consumer", queues = "history.stream", containerFactory = "nativeFactory")
    public void handleStreamMessage(@Payload byte[] messageBody) throws IOException {
        try {
            log.info("Received stream message with {} bytes", messageBody.length);
            InteractionDTO dto = objectMapper.readValue(messageBody, InteractionDTO.class);
            log.info("Deserialized stream event: {}", dto);
            log.info("Event: {}", dto.getTimestamp() + " / " + dto.getFilmId() + " / " + dto.getUserId());
            // Process your event here
            // You can call your service methods or handle the event as needed
            Interaction interaction = new Interaction(
                    dto.getUserId(),
                    dto.getFilmId(),
                    dto.getInteractionType(),
                    dto.getValue(),
                    dto.getTimestamp()
            );
            interactionRepository.save(interaction);

        } catch (IOException e) {
            log.error("Failed to deserialize stream message", e);
        } catch (Exception e) {
            log.error("Error processing stream message", e);
        }
    }

}

