package com.backend.gapfinder.models;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_tokens")
@Data
public class RefreshTokenModel extends BaseModel {


    @Column(unique = true, nullable = false)
    private String token; // UUID opaco, no es un JWT

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserModel user;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    private boolean revoked = false;
}