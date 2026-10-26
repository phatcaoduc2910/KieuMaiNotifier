package com.kieumai.notifier.business.exception;

public class SubscriptionNotFoundException extends RuntimeException {

    public SubscriptionNotFoundException(long userId, long wardId) {
        super("Subscription for user " + userId + " to ward " + wardId + " not found");
    }
}
