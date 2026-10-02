package com.backend.gapfinder.models;

import java.time.LocalDateTime;

import com.backend.gapfinder.enums.FriendshipStatusEnum;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

@Entity
@Table(
    name = "friendships",
    uniqueConstraints = @UniqueConstraint(columnNames = {"requester_id", "receiver_id"})
)
@Data

public class FriendshipModel extends BaseModel {



    // Student who sent the friend request
    @ManyToOne
    @JoinColumn(name = "requester_id", nullable = false)
    private UserModel requester;

    // Student who received the friend request
    @ManyToOne
    @JoinColumn(name = "receiver_id", nullable = false)
    private UserModel receiver;

    // Current status of the request (e.g., PENDING, ACCEPTED, REJECTED)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FriendshipStatusEnum status;

    // Date and time when the friend request was created
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}