package com.kieumai.notifier.business.exception;

public class NotificationNotFoundException extends RuntimeException {

    public NotificationNotFoundException(long notificationId) {
        super("Notification " + notificationId + " not found");
    }
}
