package com.backend.gapfinder.model.events;

import com.backend.gapfinder.model.enums.NotificationTypeEnum;

// Published when something happens to an open table (a user joined)
public record OpenTableEvent(
        NotificationTypeEnum type,
        Long recipientId,
        String actorName,
        Long openTableId,
        String openTableTitle) {
}
