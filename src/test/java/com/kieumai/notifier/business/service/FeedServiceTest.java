package com.kieumai.notifier.business.service;

import com.kieumai.notifier.business.model.ActiveIncident;
import com.kieumai.notifier.business.model.Subscription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FeedServiceTest {

    private static final long USER_ID = 7L;
    private static final long OTHER_USER_ID = 8L;
    private static final Instant NOON = Instant.parse("2026-10-27T05:00:00Z");

    private InMemorySubscriptionRepository subscriptionRepository;
    private FakeActiveIncidentRepository activeIncidentRepository;
    private FeedService service;

    @BeforeEach
    void setUp() {
        subscriptionRepository = new InMemorySubscriptionRepository();
        activeIncidentRepository = new FakeActiveIncidentRepository();
        service = new FeedService(subscriptionRepository, activeIncidentRepository);
    }

    @Test
    void getFeed_isEmptyWhenUserFollowsNoWard() {
        activeIncidentRepository.addIncident(1L, 10L, NOON);

        assertTrue(service.getFeed(USER_ID).isEmpty());
    }

    @Test
    void getFeed_returnsOnlyIncidentsOfFollowedWards() {
        subscribe(USER_ID, 10L);
        subscribe(USER_ID, 20L);
        subscribe(OTHER_USER_ID, 30L);
        activeIncidentRepository.addIncident(1L, 10L, NOON);
        activeIncidentRepository.addIncident(2L, 20L, NOON);
        activeIncidentRepository.addIncident(3L, 30L, NOON);

        List<ActiveIncident> feed = service.getFeed(USER_ID);

        assertEquals(List.of(2L, 1L), feed.stream().map(ActiveIncident::incidentId).toList());
    }

    @Test
    void getFeed_putsNewestIncidentFirstAndUndatedLast() {
        subscribe(USER_ID, 10L);
        activeIncidentRepository.addIncident(1L, 10L, NOON);
        activeIncidentRepository.addIncident(2L, 10L, NOON.plusSeconds(60));
        activeIncidentRepository.addIncident(3L, 10L, null);

        List<ActiveIncident> feed = service.getFeed(USER_ID);

        assertEquals(List.of(2L, 1L, 3L), feed.stream().map(ActiveIncident::incidentId).toList());
    }

    private void subscribe(long userId, long wardId) {
        subscriptionRepository.save(new Subscription(null, userId, wardId, NOON));
    }
}
