package com.kieumai.notifier.presentation.controller;

import com.kieumai.notifier.business.service.FeedService;
import com.kieumai.notifier.presentation.auth.CurrentUserProvider;
import com.kieumai.notifier.presentation.dto.FeedItemResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/me/feed")
public class FeedController {

    private final FeedService feedService;
    private final CurrentUserProvider currentUserProvider;

    public FeedController(FeedService feedService, CurrentUserProvider currentUserProvider) {
        this.feedService = feedService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    public List<FeedItemResponse> getFeed() {
        long userId = currentUserProvider.currentUserId();
        return feedService.getFeed(userId).stream()
                .map(FeedItemResponse::from)
                .toList();
    }
}
