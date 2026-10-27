package com.kieumai.notifier.config;

import com.kieumai.notifier.business.service.FeedService;
import com.kieumai.notifier.persistence.repository.ActiveIncidentRepository;
import com.kieumai.notifier.persistence.repository.SubscriptionRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NotifierServiceConfig {

    @Bean
    public FeedService feedService(SubscriptionRepository subscriptionRepository,
                                   ActiveIncidentRepository activeIncidentRepository) {
        return new FeedService(subscriptionRepository, activeIncidentRepository);
    }
}
