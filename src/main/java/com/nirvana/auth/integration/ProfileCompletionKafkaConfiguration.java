package com.nirvana.auth.integration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration(proxyBeanMethods = false)
class ProfileCompletionKafkaConfiguration {
    @Bean
    DefaultErrorHandler profileCompletionErrorHandler() {
        return new DefaultErrorHandler(new FixedBackOff(5_000, FixedBackOff.UNLIMITED_ATTEMPTS));
    }
}
