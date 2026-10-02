package com.backend.gapfinder.models;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;


@Entity
@Table(name = "participants_open_tables")

@Data

public class OpenTableParticipantModel extends BaseModel {



    
    // The open table session joined by the student
    @ManyToOne
    @JoinColumn(name = "open_table_id", nullable = false)
    private OpenTableModel openTable;

    // Student who joined the open table
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserModel user;

    // Date and time when the student joined
    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;
}