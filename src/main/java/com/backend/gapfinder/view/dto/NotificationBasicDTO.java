package com.backend.gapfinder.view.dto;

import com.backend.gapfinder.model.enums.NotificationTypeEnum;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class NotificationBasicDTO {

    private Long id;
    private NotificationTypeEnum type;
    private String message;
    private Long relatedEntityId;
    private boolean read;
    private LocalDateTime createdAt;
}
