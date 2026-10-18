package com.backend.gapfinder.model.events;

import com.backend.gapfinder.model.enums.NotificationTypeEnum;

// Published when something happens to a friendship (sent, accepted, rejected)
public record FriendshipEvent(
        NotificationTypeEnum type,
        Long recipientId,
        String actorName,
        Long friendshipId) {
}
