package com.backend.gapfinder.dto;

import lombok.Data;

// Minimal user data shown inside other resources
@Data
public class UserSummaryDTO {
    private Long id;
    private String name;
    private String career;
    private String avatarUrl;
}