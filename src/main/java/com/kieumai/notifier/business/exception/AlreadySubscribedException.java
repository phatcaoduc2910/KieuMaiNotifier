package com.kieumai.notifier.business.exception;

public class AlreadySubscribedException extends RuntimeException {

    public AlreadySubscribedException(long userId, long wardId) {
        super("User " + userId + " already subscribes to ward " + wardId);
    }

}
