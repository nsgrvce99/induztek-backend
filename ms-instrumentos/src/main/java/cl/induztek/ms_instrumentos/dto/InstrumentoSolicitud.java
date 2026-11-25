package cl.induztek.ms_instrumentos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InstrumentoSolicitud(
        @NotBlank @Size(max = 120) String nombre,
        @NotBlank @Size(max = 120) String marca,
        @NotBlank @Size(max = 100) String serie,
        @NotBlank String fechaVencimiento) {}