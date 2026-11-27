package com.nirvana.auth.integration;

import com.nirvana.auth.service.UserAccountService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
class ProfileCreatedListener {
    private final ObjectMapper json;
    private final UserAccountService accounts;

    @KafkaListener(topics = "${auth.profile-completion.topic}", groupId = "${auth.profile-completion.group-id}")
    void receive(ConsumerRecord<String, String> record) {
        if (record.value() == null) {
            return;
        }
        ProfileCreated event;
        try {
            event = json.readValue(record.value(), ProfileCreated.class);
        } catch (JacksonException exception) {
            logInvalidRecord(record);
            return;
        }
        if (event == null || !"ProfileCreated".equals(event.eventType()) || event.userUuid() == null
                || event.requestEventId() == null || !event.userUuid().toString().equals(record.key())) {
            logInvalidRecord(record);
            return;
        }
        accounts.activateRegistration(event.userUuid());
    }

    private void logInvalidRecord(ConsumerRecord<String, String> record) {
        log.warn("Discarding invalid profile completion. topic={} partition={} offset={}",
                record.topic(), record.partition(), record.offset());
    }

    private record ProfileCreated(String eventType, UUID userUuid, UUID requestEventId) {
    }
}
