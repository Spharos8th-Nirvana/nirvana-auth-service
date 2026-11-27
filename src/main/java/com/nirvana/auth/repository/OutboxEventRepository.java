package com.nirvana.auth.repository;

import com.nirvana.auth.domain.OutboxEvent;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    @Query(value = """
            select * from outboxEvents
            where status = 'PENDING' and eventType = :eventType and nextAttemptAt <= :now
            order by nextAttemptAt, id
            limit 1 for update skip locked
            """, nativeQuery = true)
    Optional<OutboxEvent> lockNextPending(@Param("eventType") String eventType, @Param("now") LocalDateTime now);
}
