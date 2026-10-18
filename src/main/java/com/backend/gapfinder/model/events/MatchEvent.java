package com.backend.gapfinder.model.events;

import com.backend.gapfinder.model.enums.NotificationTypeEnum;

// Published when something happens to a match (proposed, accepted, rejected)
public record MatchEvent(
        NotificationTypeEnum type,
        Long recipientId,
        String actorName,
        Long matchId) {
}
