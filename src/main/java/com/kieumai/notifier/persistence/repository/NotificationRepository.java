package com.kieumai.notifier.persistence.repository;

import com.kieumai.notifier.business.model.Notification;

import java.time.Instant;
import java.util.List;

public interface NotificationRepository {

    void saveAll(List<Notification> notifications);

    List<Notification> findByUserId(long userId, boolean unreadOnly);

    boolean markAsRead(long notificationId, long userId, Instant readAt);
}
