package cl.induztek.ms_usuarios.security;

import cl.induztek.ms_usuarios.dto.ActivacionRequest;
import cl.induztek.ms_usuarios.dto.CambiarContrasenaRequest;
import cl.induztek.ms_usuarios.dto.LoginRequest;
import cl.induztek.ms_usuarios.dto.LoginResponse;
import cl.induztek.ms_usuarios.dto.PersonalCreadoResponse;
import cl.induztek.ms_usuarios.dto.FotoRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

class CredencialesEnLogsTest {
    @Test
    void omiteCredencialesYContenidoDeFotosAlRepresentarLosDto() {
        var sensible = "valor-privado-de-prueba";
        var dtos = List.of(
                new ActivacionRequest(sensible), new CambiarContrasenaRequest(sensible, sensible, sensible),
                new LoginRequest("demo@example.invalid", sensible),
                new LoginResponse(sensible, "Bearer", 1L, 1L, "Tecnico", false, null),
                new PersonalCreadoResponse(null, sensible), new FotoRequest(sensible));
        for (var dto : dtos) {
            assertFalse(dto.toString().contains(sensible));
        }
    }
}
