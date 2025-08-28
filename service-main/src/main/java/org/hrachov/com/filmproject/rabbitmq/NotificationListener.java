package org.hrachov.com.filmproject.rabbitmq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.client.MongoClients;
import lombok.AllArgsConstructor;
import org.hrachov.com.filmproject.controller.NotificationWebSocketController;
import org.hrachov.com.filmproject.model.notification.Notification;
import org.hrachov.com.filmproject.model.notification.NotificationDTO;
import org.hrachov.com.filmproject.repository.mongo.NotificationRepository;
import org.hrachov.com.filmproject.service.UserService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@AllArgsConstructor
public class NotificationListener {
    private final NotificationRepository notificationRepository;
    //TODO for now when compressed service active we send only uuid ,
    // should also send userId because userId in main service could be null or irrelevant
    private final NotificationWebSocketController notificationWebSocketController;
    private final UserService userService;

    //TODO WORKS FOR COMPRESSION MICROSERVICE
    @RabbitListener(queues = "notification.queue")
    public void listen(NotificationDTO notificationDTO) throws JsonProcessingException {
        //FOR NOW EMPTY
        Notification notification = new Notification();
        notification.setUserId(userService.findByUsername(notificationDTO.getUsername()).getId());
        notification.setMessage(notificationDTO.getMessage());
        notification.setRead(notificationDTO.isRead());
        notification.setType(String.valueOf(notificationDTO.getType()));
        notification.setCreatedAt(LocalDateTime.now());
        notification.setLink(notificationDTO.getLink());

        //TODO была проблема с localdate и jackson
        notificationRepository.save(notification);
        notificationWebSocketController.sendNotification(notificationDTO.getUsername(),notificationDTO.getMessage());
        System.out.println("Пришло сообщение =" + notificationDTO);

    }
}
