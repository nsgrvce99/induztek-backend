package cl.induztek.ms_usuarios.dto;

import cl.induztek.ms_usuarios.validation.RutValido;
import jakarta.validation.constraints.*;

public record PersonalRequest(

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 150)
    @Pattern(
        regexp = "^[A-Za-z0-9áéíóúÁÉÍÓÚñÑ\\s.,-]+$",
        message = "El nombre contiene caracteres inválidos"
    )
    String nombre,

    @NotBlank(message = "El RUT es obligatorio")
    @Size(max = 20)
    @RutValido
    String rut,

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "Correo electrónico inválido")
    @Size(max = 150)
    String correo,

    @NotNull(message = "El rol es obligatorio")
    @Min(value = 1, message = "Rol inválido")
    @Max(value = 3, message = "Rol inválido")
    Long rolId

) {}