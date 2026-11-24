package com.backend.gapfinder.view.dto.requests;

import com.backend.gapfinder.model.enums.EffortTypeEnum;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// Datos que envía el cliente para crear una cuenta nueva
public record RegisterRequest(

    // Nombre completo del estudiante (solo letras y un espacio entre palabras)
    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 50, message = "El nombre debe tener entre 2 y 50 caracteres")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚáéíóúÑñÜü]+( [A-Za-zÁÉÍÓÚáéíóúÑñÜü]+)*$",
            message = "El nombre solo puede tener letras y espacios")
    String name,

    // Correo usado como identificador de login (solo @gmail.com o @outlook.com)
    @NotBlank(message = "El correo es obligatorio")
    @Size(max = 100, message = "El correo es demasiado largo")
    @Pattern(regexp = "^[a-z0-9]+([._-][a-z0-9]+)*@(gmail|outlook)\\.com$",
            flags = Pattern.Flag.CASE_INSENSITIVE,
            message = "Ingrese una dirección válida (@gmail.com o @outlook.com)")
    String email,

    // Contraseña en texto plano; el servidor la guarda como hash
    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, max = 64, message = "La contraseña debe tener entre 8 y 64 caracteres")
    @Pattern(regexp = "^[\\x21-\\x7E]+$",
            message = "La contraseña no puede tener espacios, tildes ni emojis")
    String password,

    // Carrera o programa académico (mapea a UserModel.career)
    @NotBlank(message = "La carrera es obligatoria")
    @Size(max = 100, message = "La carrera no es válida")
    String career,

    // Semestre actual del estudiante
    @NotNull(message = "El semestre es obligatorio")
    @Min(value = 1, message = "El semestre debe ser al menos 1")
    @Max(value = 12, message = "El semestre no puede ser mayor a 12")
    Integer semester,

    // Celular colombiano: 10 dígitos que empiezan por 3
    @NotBlank(message = "El teléfono es obligatorio")
    @Pattern(regexp = "^3[0-9]{9}$",
            message = "El teléfono debe tener 10 dígitos y empezar por 3")
    String phoneNumber,

    // Nivel de esfuerzo preferido para actividades (mapea a UserModel.preferredEffort)
    @NotNull(message = "La preferencia de esfuerzo es obligatoria")
    EffortTypeEnum preferredEffort
) {}