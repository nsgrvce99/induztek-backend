package cl.induztek.ms_usuarios.dto;

import jakarta.validation.constraints.Size;

public record FotoRequest(

    @Size(max = 2000)
    String fotoUri

) {}