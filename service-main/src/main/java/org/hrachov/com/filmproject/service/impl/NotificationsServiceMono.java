package org.hrachov.com.filmproject.service.impl;

import org.hrachov.com.filmproject.model.notification.Notification;
import org.hrachov.com.filmproject.repository.mongo.NotificationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationsServiceMono {
    private final NotificationRepository notificationRepository;

    public NotificationsServiceMono(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public void markAsRead(String id) {
        notificationRepository.findById(id).ifPresent(notification -> {
            notification.setRead(true);
            notification.setCreatedAt(LocalDateTime.now().plusSeconds(60));
            notificationRepository.save(notification);
        });
    }

    public Notification createNotification(Notification notification) {
        notification.setRead(true);
        notification.setCreatedAt(LocalDateTime.now().plusSeconds(60));
        notification.setUserId(notification.getUserId());
        notification.setLink(notification.getLink());
        notification.setMessage(notification.getMessage());
        notification.setType(notification.getType());

        return notificationRepository.save(notification);
    }

    public List<Notification> getAllNotifications(Long userId) {
        return notificationRepository.getAllByUserId(userId);
    }
}
