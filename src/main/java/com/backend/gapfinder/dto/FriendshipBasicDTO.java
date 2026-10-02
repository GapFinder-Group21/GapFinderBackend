package com.backend.gapfinder.dto;

import java.time.LocalDateTime;

import com.backend.gapfinder.enums.FriendshipStatusEnum;

import lombok.Data;


@Data
public class FriendshipBasicDTO {

    // Unique friendship record identifier
    private Long id;

    // Current status of the friendship request
    private FriendshipStatusEnum status;

    // Date and time when the friendship request was created
    private LocalDateTime createdAt;

    // Id of the student who sent the request
    private Long requesterId;

    // Id of the student who received the request
    private Long receiverId;
}