package com.kieumai.notifier.business.service;

import com.kieumai.notifier.business.exception.AlreadySubscribedException;
import com.kieumai.notifier.business.exception.SubscriptionNotFoundException;
import com.kieumai.notifier.business.exception.WardNotFoundException;
import com.kieumai.notifier.business.model.Subscription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SubscriptionServiceTest {

    private InMemorySubscriptionRepository subscriptionRepository;
    private SubscriptionService service;

    @BeforeEach
    void setUp() {
        subscriptionRepository = new InMemorySubscriptionRepository();
        service = new SubscriptionService(subscriptionRepository, new FakeWardRepository(1L, 2L));
    }

    @Test
    void subscribe_savesNewSubscription() {
        Subscription result = service.subscribe(10L, 1L);

        assertNotNull(result.id());
        assertEquals(10L, result.userId());
        assertEquals(1L, result.wardId());
        assertTrue(subscriptionRepository.existsByUserIdAndWardId(10L, 1L));
    }

    @Test
    void subscribe_throwsWhenAlreadySubscribed() {
        service.subscribe(10L, 1L);

        assertThrows(AlreadySubscribedException.class, () -> service.subscribe(10L, 1L));
    }

    @Test
    void subscribe_throwsWhenWardNotFound() {
        assertThrows(WardNotFoundException.class, () -> service.subscribe(10L, 99L));
    }

    @Test
    void unsubscribe_throwsWhenNotSubscribed() {
        assertThrows(SubscriptionNotFoundException.class, () -> service.unsubscribe(10L, 1L));
    }

    @Test
    void unsubscribe_removesSubscription() {
        service.subscribe(10L, 1L);

        service.unsubscribe(10L, 1L);

        List<Subscription> list = service.getSubscriptions(10L);
        assertTrue(list.isEmpty());
    }
}
