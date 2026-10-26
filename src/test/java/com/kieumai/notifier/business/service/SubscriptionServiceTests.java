package com.kieumai.notifier.business.service;

import com.kieumai.notifier.business.exception.AlreadySubscribedException;
import com.kieumai.notifier.business.exception.SubscriptionNotFoundException;
import com.kieumai.notifier.business.exception.WardNotFoundException;
import com.kieumai.notifier.business.model.Subscription;
import com.kieumai.notifier.persistence.repository.SubscriptionRepository;
import com.kieumai.notifier.persistence.repository.WardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SubscriptionServiceTests {
    private SubscriptionRepository subscriptions;
    private WardRepository wards;
    private SubscriptionService service;

    @BeforeEach
    void setUp() {
        subscriptions = mock(SubscriptionRepository.class);
        wards = mock(WardRepository.class);
        service = new SubscriptionService(subscriptions, wards);
    }

    @Test
    void subscriptionToUnknownWardIsRejectedBeforeSaving() {
        when(wards.existsById(99L)).thenReturn(false);
        assertThrows(WardNotFoundException.class, () -> service.subscribe(1L, 99L));
        verifyNoInteractions(subscriptions);
    }

    @Test
    void duplicateSubscriptionIsRejectedWithoutSaving() {
        when(wards.existsById(2L)).thenReturn(true);
        when(subscriptions.existsByUserIdAndWardId(1L, 2L)).thenReturn(true);
        assertThrows(AlreadySubscribedException.class, () -> service.subscribe(1L, 2L));
        verify(subscriptions, never()).save(any());
    }

    @Test
    void subscriptionPreservesUserWardAndCreationTime() {
        when(wards.existsById(2L)).thenReturn(true);
        when(subscriptions.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        Subscription result = service.subscribe(1L, 2L);
        assertEquals(1L, result.userId());
        assertEquals(2L, result.wardId());
        assertNotNull(result.createdAt());
    }

    @Test
    void unsubscribeRejectsMissingSubscriptionForRequestedUser() {
        when(subscriptions.deleteByUserIdAndWardId(1L, 2L)).thenReturn(false);
        assertThrows(SubscriptionNotFoundException.class, () -> service.unsubscribe(1L, 2L));
    }

    @Test
    void existingSubscriptionCanBeRemoved() {
        when(subscriptions.deleteByUserIdAndWardId(1L, 2L)).thenReturn(true);
        assertDoesNotThrow(() -> service.unsubscribe(1L, 2L));
    }

    @Test
    void listIsScopedToTheRequestedUser() {
        Subscription item = new Subscription(3L, 1L, 2L, Instant.now());
        when(subscriptions.findByUserId(1L)).thenReturn(List.of(item));
        assertEquals(List.of(item), service.getSubscriptions(1L));
    }
}
