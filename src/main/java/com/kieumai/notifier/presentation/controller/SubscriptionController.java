package com.kieumai.notifier.presentation.controller;

import com.kieumai.notifier.business.service.SubscriptionService;
import com.kieumai.notifier.presentation.auth.CurrentUserProvider;
import com.kieumai.notifier.presentation.dto.SubscribeRequest;
import com.kieumai.notifier.presentation.dto.SubscriptionResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/me/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final CurrentUserProvider currentUserProvider;

    public SubscriptionController(SubscriptionService subscriptionService,
                                  CurrentUserProvider currentUserProvider) {
        this.subscriptionService = subscriptionService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SubscriptionResponse subscribe(@Valid @RequestBody SubscribeRequest request) {
        long userId = currentUserProvider.currentUserId();
        return SubscriptionResponse.from(subscriptionService.subscribe(userId, request.wardId()));
    }

    @GetMapping
    public List<SubscriptionResponse> getSubscriptions() {
        long userId = currentUserProvider.currentUserId();
        return subscriptionService.getSubscriptions(userId).stream()
                .map(SubscriptionResponse::from)
                .toList();
    }

    @DeleteMapping("/{wardId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unsubscribe(@PathVariable long wardId) {
        long userId = currentUserProvider.currentUserId();
        subscriptionService.unsubscribe(userId, wardId);
    }
}
