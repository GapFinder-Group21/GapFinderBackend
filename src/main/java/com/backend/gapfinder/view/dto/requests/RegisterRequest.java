package com.backend.gapfinder.dto.requests;

import com.backend.gapfinder.enums.EffortTypeEnum;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// Datos que envía el cliente para crear una cuenta nueva
public record RegisterRequest(

    // Nombre completo del estudiante
    @NotBlank(message = "El nombre es obligatorio")
    String name,

    // Correo usado como identificador de login (se guarda en minúsculas)
    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no es válido")
    String email,

    // Contraseña en texto plano; el servidor la guarda como hash
    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    String password,

    // Carrera o programa académico (mapea a UserModel.career)
    @NotBlank(message = "La carrera es obligatoria")
    String career,

    // Semestre actual del estudiante
    @NotNull(message = "El semestre es obligatorio")
    @Min(value = 1, message = "El semestre debe ser al menos 1")
    @Max(value = 12, message = "El semestre no puede ser mayor a 12")
    Integer semester,

    // Teléfono de contacto (opcional; solo dígitos, con + opcional al inicio)
    @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "El teléfono no es válido")
    String phoneNumber,

    // Nivel de esfuerzo preferido para actividades (mapea a UserModel.preferredEffort)
    @NotNull(message = "La preferencia de esfuerzo es obligatoria")
    EffortTypeEnum preferredEffort
) {}