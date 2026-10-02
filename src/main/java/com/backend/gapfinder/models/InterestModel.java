package com.backend.gapfinder.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "interests")
@Data

public class InterestModel extends BaseModel{


    // Name of the interest or hobby (e.g., Gaming, Sports, Music)
    @Column(nullable = false, unique = true)
    private String name;
}