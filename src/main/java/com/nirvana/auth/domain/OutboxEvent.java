package com.nirvana.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "outboxEvents")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false, length = 36)
    private String eventId;

    @Column(nullable = false, updatable = false, length = 50)
    private String aggregateType;

    @Getter
    @Column(nullable = false, updatable = false, length = 36)
    private String aggregateUuid;

    @Column(nullable = false, updatable = false, length = 80)
    private String eventType;

    @Getter
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, updatable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(nullable = false)
    private int attemptCount;

    @Column(nullable = false)
    private LocalDateTime nextAttemptAt;

    @Column(length = 500)
    private String lastError;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime publishedAt;

    public OutboxEvent(String eventId, String aggregateType, String aggregateUuid, String eventType,
                       String payload, LocalDateTime now) {
        this.eventId = eventId;
        this.aggregateType = aggregateType;
        this.aggregateUuid = aggregateUuid;
        this.eventType = eventType;
        this.payload = payload;
        this.status = Status.PENDING;
        this.nextAttemptAt = now;
        this.createdAt = now;
    }

    public void markPublished(LocalDateTime now) {
        status = Status.PUBLISHED;
        publishedAt = now;
        lastError = null;
    }

    public void scheduleRetry(LocalDateTime now, String errorType) {
        attemptCount++;
        long delaySeconds = Math.min(300, 1L << Math.min(attemptCount, 9));
        nextAttemptAt = now.plusSeconds(delaySeconds);
        lastError = errorType;
    }

    private enum Status {
        PENDING, PUBLISHED, FAILED
    }
}
