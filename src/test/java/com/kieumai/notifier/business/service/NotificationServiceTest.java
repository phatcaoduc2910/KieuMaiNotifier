package com.kieumai.notifier.business.service;

import com.kieumai.notifier.business.exception.NotificationNotFoundException;
import com.kieumai.notifier.business.model.Notification;
import com.kieumai.notifier.business.model.Subscription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class NotificationServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-27T03:00:00Z");

    private InMemorySubscriptionRepository subscriptionRepository;
    private InMemoryNotificationRepository notificationRepository;
    private FakeIncidentAlertRepository incidentAlertRepository;
    private NotificationService service;

    @BeforeEach
    void setUp() {
        subscriptionRepository = new InMemorySubscriptionRepository();
        notificationRepository = new InMemoryNotificationRepository();
        incidentAlertRepository = new FakeIncidentAlertRepository();
        service = new NotificationService(subscriptionRepository, notificationRepository,
                incidentAlertRepository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void dispatch_notifiesEverySubscriberOfTheIncidentWard() {
        subscribe(10L, 1L);
        subscribe(20L, 1L);
        subscribe(30L, 2L);
        incidentAlertRepository.addActiveIncident(100L, 1L, "FLOOD", "Ngập sâu đường Xuân Thủy");

        int created = service.dispatchPendingAlerts();

        assertEquals(2, created);
        assertEquals(1, service.getNotifications(10L, false).size());
        assertEquals(1, service.getNotifications(20L, false).size());
        assertTrue(service.getNotifications(30L, false).isEmpty());
        Notification notification = service.getNotifications(10L, false).get(0);
        assertEquals(100L, notification.incidentId());
        assertEquals("Ngập sâu đường Xuân Thủy", notification.message());
        assertEquals(NOW, notification.createdAt());
    }

    @Test
    void dispatch_alertsEachIncidentOnlyOnce() {
        subscribe(10L, 1L);
        incidentAlertRepository.addActiveIncident(100L, 1L, "FLOOD", "Ngập");

        service.dispatchPendingAlerts();
        int secondRun = service.dispatchPendingAlerts();

        assertEquals(0, secondRun);
        assertEquals(1, notificationRepository.size());
    }

    @Test
    void dispatch_marksIncidentAlertedEvenWithoutSubscribers() {
        incidentAlertRepository.addActiveIncident(100L, 1L, "FLOOD", "Ngập");

        assertEquals(0, service.dispatchPendingAlerts());
        assertTrue(incidentAlertRepository.isAlerted(100L));
    }

    @Test
    void markAsRead_hidesNotificationFromUnreadList() {
        subscribe(10L, 1L);
        incidentAlertRepository.addActiveIncident(100L, 1L, "FLOOD", "Ngập");
        service.dispatchPendingAlerts();
        long notificationId = service.getNotifications(10L, false).get(0).id();

        service.markAsRead(10L, notificationId);

        assertTrue(service.getNotifications(10L, true).isEmpty());
        assertEquals(NOW, service.getNotifications(10L, false).get(0).readAt());
    }

    @Test
    void markAsRead_throwsForAnotherUsersNotification() {
        subscribe(10L, 1L);
        incidentAlertRepository.addActiveIncident(100L, 1L, "FLOOD", "Ngập");
        service.dispatchPendingAlerts();
        long notificationId = service.getNotifications(10L, false).get(0).id();

        assertThrows(NotificationNotFoundException.class, () -> service.markAsRead(20L, notificationId));
    }

    private void subscribe(long userId, long wardId) {
        subscriptionRepository.save(new Subscription(null, userId, wardId, NOW));
    }
}
