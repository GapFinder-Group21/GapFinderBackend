package com.backend.gapfinder.dto;

import lombok.Data;

@Data
public class FriendshipCompleteDTO extends FriendshipBasicDTO {

    // Student who sent the friendship request
    private UserBasicDTO requester;

    // Student who received the friendship request
    private UserBasicDTO receiver;
}
