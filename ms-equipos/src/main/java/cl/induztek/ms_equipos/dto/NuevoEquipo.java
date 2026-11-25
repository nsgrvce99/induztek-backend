package cl.induztek.ms_equipos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record NuevoEquipo(
        @NotBlank @Size(max = 60) @Pattern(regexp = "[A-Z0-9-]+") String tag,
        @NotBlank @Size(max = 80) String tipoEquipo,
        @NotBlank @Size(max = 80) String marca,
        @NotBlank @Size(max = 100) String serie,
        @Positive long instalacionId
) {}
