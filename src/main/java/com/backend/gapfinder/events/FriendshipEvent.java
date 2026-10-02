package com.backend.gapfinder.events;

import com.backend.gapfinder.enums.NotificationTypeEnum;

// Published when something happens to a friendship (sent, accepted, rejected)
public record FriendshipEvent(
        NotificationTypeEnum type,
        Long recipientId,
        String actorName,
        Long friendshipId) {
}
