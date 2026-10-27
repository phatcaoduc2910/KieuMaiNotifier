package com.kieumai.notifier.business.service;

import com.kieumai.notifier.business.exception.NotificationNotFoundException;
import com.kieumai.notifier.business.model.IncidentAlert;
import com.kieumai.notifier.business.model.Notification;
import com.kieumai.notifier.persistence.repository.IncidentAlertRepository;
import com.kieumai.notifier.persistence.repository.NotificationRepository;
import com.kieumai.notifier.persistence.repository.SubscriptionRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

public class NotificationService {

    private final SubscriptionRepository subscriptionRepository;
    private final NotificationRepository notificationRepository;
    private final IncidentAlertRepository incidentAlertRepository;
    private final Clock clock;

    public NotificationService(SubscriptionRepository subscriptionRepository,
                               NotificationRepository notificationRepository,
                               IncidentAlertRepository incidentAlertRepository,
                               Clock clock) {
        this.subscriptionRepository = subscriptionRepository;
        this.notificationRepository = notificationRepository;
        this.incidentAlertRepository = incidentAlertRepository;
        this.clock = clock;
    }

    public int dispatchPendingAlerts() {
        int created = 0;
        for (IncidentAlert alert : incidentAlertRepository.findActiveNotAlerted()) {
            Instant now = clock.instant();
            if (!incidentAlertRepository.markAlerted(alert.incidentId(), now)) {
                continue;
            }
            List<Long> subscriberIds = subscriptionRepository.findUserIdsByWardId(alert.wardId());
            if (subscriberIds.isEmpty()) {
                continue;
            }
            String title = titleFor(alert.type());
            String message = messageFor(alert);
            List<Notification> notifications = subscriberIds.stream()
                    .map(userId -> new Notification(null, userId, alert.incidentId(), title, message, now, null))
                    .toList();
            notificationRepository.saveAll(notifications);
            created += notifications.size();
        }
        return created;
    }

    public List<Notification> getNotifications(long userId, boolean unreadOnly) {
        return notificationRepository.findByUserId(userId, unreadOnly);
    }

    public void markAsRead(long userId, long notificationId) {
        boolean found = notificationRepository.markAsRead(notificationId, userId, clock.instant());
        if (!found) {
            throw new NotificationNotFoundException(notificationId);
        }
    }

    private static String titleFor(String incidentType) {
        if ("FLOOD".equals(incidentType)) {
            return "Cảnh báo ngập tại phường bạn theo dõi";
        }
        if ("TRAFFIC_JAM".equals(incidentType)) {
            return "Cảnh báo ùn tắc tại phường bạn theo dõi";
        }
        return "Sự cố mới tại phường bạn theo dõi";
    }

    private static String messageFor(IncidentAlert alert) {
        if (alert.title() == null || alert.title().isBlank()) {
            return "Có sự cố mới đã được xác nhận. Mở sự cố #" + alert.incidentId() + " để xem chi tiết.";
        }
        return alert.title();
    }
}
