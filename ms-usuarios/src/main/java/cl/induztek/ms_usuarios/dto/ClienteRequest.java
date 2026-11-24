package cl.induztek.ms_usuarios.dto;

import cl.induztek.ms_usuarios.validation.RutValido;
import jakarta.validation.constraints.*;

public record ClienteRequest(

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 150)
    String nombre,

    @NotBlank(message = "El RUT es obligatorio")
    @Size(max = 20)
    @RutValido
    String rut,

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "Correo electrónico inválido")
    @Size(max = 150)
    String correo,

    @Size(max = 150)
    String giro

) {}