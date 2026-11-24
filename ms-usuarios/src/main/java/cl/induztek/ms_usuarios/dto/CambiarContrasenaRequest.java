package cl.induztek.ms_usuarios.dto;

import jakarta.validation.constraints.*;

public record CambiarContrasenaRequest(

    @NotBlank(message = "La contraseña actual es obligatoria")
    @Size(max = 72)
    String actual,

    @NotBlank(message = "La nueva contraseña es obligatoria")
    @Size(min = 6, max = 18)
    @Pattern(
        regexp = "^(?=.*[\\p{Lu}])(?=.*[^\\p{L}\\p{N}\\s]).+$",
        message = "Debe contener una mayúscula y un carácter especial"
    )
    String nueva,

    @NotBlank(message = "Debe confirmar la contraseña")
    String confirmar

) {}