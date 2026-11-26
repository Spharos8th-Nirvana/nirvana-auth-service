package com.nirvana.auth.dto.event;

public record ProfileCreationRequested(String eventId, String eventType, String userUuid,
                                       String name, String countryCode) {
    public static final String TYPE = "ProfileCreationRequested";
}
