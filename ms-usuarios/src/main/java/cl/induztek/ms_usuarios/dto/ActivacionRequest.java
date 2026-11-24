package cl.induztek.ms_usuarios.dto;

import jakarta.validation.constraints.*;

public record ActivacionRequest(

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(
        min = 6,
        max = 18,
        message = "La contraseña debe tener entre 6 y 18 caracteres"
    )
    String contrasena

) {}