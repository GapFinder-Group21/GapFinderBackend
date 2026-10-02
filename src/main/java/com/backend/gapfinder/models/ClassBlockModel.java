package com.backend.gapfinder.models;

import com.backend.gapfinder.enums.DayOfWeekEnum;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalTime;

// Entidad que representa un bloque de clase dentro del horario de un usuario
@Entity
@Table(name = "class_blocks")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ClassBlockModel extends BaseModel {

    private String subject;

    private String location;

    @Enumerated(EnumType.STRING)
    private DayOfWeekEnum dayOfWeek;

    private LocalTime startTime;

    private LocalTime endTime;

    // Usuario dueño de esta clase
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private UserModel user;

}