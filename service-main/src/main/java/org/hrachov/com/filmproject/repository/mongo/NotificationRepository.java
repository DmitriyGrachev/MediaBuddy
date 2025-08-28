package org.hrachov.com.filmproject.repository.mongo;

import org.hrachov.com.filmproject.model.notification.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {
    List<Notification> findByReadFalse();
    List<Notification> getAllByUserId(Long userId);
}
