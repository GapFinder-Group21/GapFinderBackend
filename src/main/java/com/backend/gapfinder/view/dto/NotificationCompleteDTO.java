package com.backend.gapfinder.view.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class NotificationCompleteDTO extends NotificationBasicDTO {

    // User who receives the notification
    private UserBasicDTO recipient;
}