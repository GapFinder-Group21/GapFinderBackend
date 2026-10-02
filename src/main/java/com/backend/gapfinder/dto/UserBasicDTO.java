package com.backend.gapfinder.dto;
import lombok.Data;
import java.time.LocalDateTime;
import com.backend.gapfinder.enums.EffortTypeEnum;
// Basic DTO (Data Transfer Object) representing fundamental user information.
@Data
public class UserBasicDTO {

    // Unique identifier for the user.
    private Long id;

    // Full name or display name of the user.
    private String name;

    // User's email address.
    private String email;

    // Contact phone number.
    private String phoneNumber;

    // Major or academic program the user is enrolled in.
    private String career;

    // Current semester the user is in.
    private Integer semester;

    // URL of the user's profile picture/avatar.
    private String avatarUrl;

    // Date and time when the user account was created.
    private LocalDateTime createdAt;


}