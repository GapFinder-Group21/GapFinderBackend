package com.backend.gapfinder.models;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;

// Superclase base con la llave primaria para todas las entidades
@Data // Genera getters, setters, toString, equals y hashCode
@MappedSuperclass // Define que sus atributos serán heredados por las tablas fijas en BD
public abstract class BaseModel {

    @Id // Define la clave primaria (PK)
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Id autoincremental en la BD
    private Long id;
}