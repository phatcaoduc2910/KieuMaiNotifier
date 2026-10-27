package com.kieumai.notifier.presentation.auth;

public interface CurrentUserProvider {

    CurrentUser currentUser();

    default long currentUserId() {
        return currentUser().userId();
    }
}
