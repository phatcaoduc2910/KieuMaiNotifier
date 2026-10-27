package com.kieumai.notifier.business.service;

import com.kieumai.notifier.business.model.Notification;
import com.kieumai.notifier.persistence.repository.NotificationRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

class InMemoryNotificationRepository implements NotificationRepository {

    private final List<Notification> rows = new ArrayList<>();
    private long nextId = 1;

    @Override
    public void saveAll(List<Notification> notifications) {
        for (Notification n : notifications) {
            rows.add(new Notification(nextId++, n.userId(), n.incidentId(), n.title(), n.message(),
                    n.createdAt(), n.readAt()));
        }
    }

    @Override
    public List<Notification> findByUserId(long userId, boolean unreadOnly) {
        return rows.stream()
                .filter(n -> n.userId() == userId)
                .filter(n -> !unreadOnly || n.readAt() == null)
                .sorted(Comparator.comparing(Notification::createdAt).reversed())
                .toList();
    }

    @Override
    public boolean markAsRead(long notificationId, long userId, Instant readAt) {
        for (int i = 0; i < rows.size(); i++) {
            Notification n = rows.get(i);
            if (n.id() == notificationId && n.userId() == userId) {
                if (n.readAt() == null) {
                    rows.set(i, new Notification(n.id(), n.userId(), n.incidentId(), n.title(), n.message(),
                            n.createdAt(), readAt));
                }
                return true;
            }
        }
        return false;
    }

    int size() {
        return rows.size();
    }
}
