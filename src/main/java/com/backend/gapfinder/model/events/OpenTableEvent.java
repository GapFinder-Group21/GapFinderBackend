package com.backend.gapfinder.events;

import com.backend.gapfinder.enums.NotificationTypeEnum;

// Published when something happens to an open table (a user joined)
public record OpenTableEvent(
        NotificationTypeEnum type,
        Long recipientId,
        String actorName,
        Long openTableId,
        String openTableTitle) {
}
