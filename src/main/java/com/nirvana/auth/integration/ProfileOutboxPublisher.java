package com.nirvana.auth.integration;

import com.nirvana.auth.domain.OutboxEvent;
import com.nirvana.auth.dto.event.ProfileCreationRequested;
import com.nirvana.auth.repository.OutboxEventRepository;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "auth.outbox", name = "publishing-enabled", matchIfMissing = true)
@EnableScheduling
class ProfileOutboxPublisher {
    private static final int BATCH_SIZE = 20;
    private final OutboxEventRepository outbox;
    private final KafkaTemplate<String, String> kafka;
    private final TransactionTemplate transaction;
    private final String topic;

    ProfileOutboxPublisher(OutboxEventRepository outbox, KafkaTemplate<String, String> kafka,
                           PlatformTransactionManager manager,
                           @Value("${auth.outbox.profile-topic}") String topic) {
        this.outbox = outbox;
        this.kafka = kafka;
        this.topic = topic;
        this.transaction = new TransactionTemplate(manager);
        this.transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
    }

    @Scheduled(fixedDelayString = "${auth.outbox.publish-interval-ms}")
    void publishPending() {
        for (int i = 0; i < BATCH_SIZE; i++) {
            if (!Boolean.TRUE.equals(transaction.execute(status -> outbox
                    .lockNextPending(ProfileCreationRequested.TYPE, LocalDateTime.now(ZoneOffset.UTC))
                    .map(this::publish)
                    .orElse(false)))) {
                return;
            }
        }
    }

    private boolean publish(OutboxEvent event) {
        try {
            kafka.send(topic, event.getAggregateUuid(), event.getPayload()).get(6, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Outbox publishing interrupted", exception);
        } catch (ExecutionException | TimeoutException | RuntimeException exception) {
            event.scheduleRetry(LocalDateTime.now(ZoneOffset.UTC), exception.getClass().getSimpleName());
            return false;
        }
        event.markPublished(LocalDateTime.now(ZoneOffset.UTC));
        return true;
    }
}
