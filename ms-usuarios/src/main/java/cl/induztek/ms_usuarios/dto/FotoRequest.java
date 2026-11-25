package cl.induztek.ms_usuarios.dto;

import jakarta.validation.constraints.Size;

public record FotoRequest(

    @Size(max = 44000)
    String fotoUri

) {
    @Override
    public String toString() {
        return "FotoRequest";
    }
}