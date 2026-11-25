
package cl.induztek.ms_usuarios.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import org.springframework.security.crypto.bcrypt
    .BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

class CredencialesServiceTest {

    private CredencialesService credencialesService;

    @BeforeEach
    void configurar() {

        credencialesService =
            new CredencialesService(
                new BCryptPasswordEncoder()
            );
    }

    @Test
    @DisplayName("Codificar contrasena con BCrypt")
    void codificarContrasena() {

        String clave = "ClaveDemo!26";

        String hash =
            credencialesService.codificar(clave);

        assertNotEquals(clave, hash);

        assertTrue(
            credencialesService.verificar(clave, hash)
        );
    }

    @Test
    @DisplayName("Rechazar contrasena incorrecta")
    void rechazarContrasenaIncorrecta() {

        String hash =
            credencialesService.codificar(
                "ClaveDemo!26"
            );

        assertFalse(
            credencialesService.verificar(
                "ClaveIncorrecta", hash
            )
        );
    }

    @Test
    @DisplayName("Reconocer contrasena temporal")
    void reconocerContrasenaTemporal() {

        String hash =
            credencialesService.codificarTemporal(
                "Temporal!26"
            );

        assertTrue(
            credencialesService.requiereCambio(hash)
        );

        assertTrue(
            credencialesService.verificar(
                "Temporal!26", hash
            )
        );
    }

    @Test
    @DisplayName("Reconocer contrasena normal")
    void reconocerContrasenaNormal() {

        String hash =
            credencialesService.codificar(
                "ClaveDemo!26"
            );

        assertFalse(
            credencialesService.requiereCambio(hash)
        );
    }

    @Test
    @DisplayName("Rechazar contrasenas superiores a 72 bytes")
    void rechazarContrasenaDemasiadoLarga() {

        String hash =
            credencialesService.codificar(
                "ClaveDemo!26"
            );

        String claveLarga = "A".repeat(73);

        assertFalse(
            credencialesService.verificar(
                claveLarga, hash
            )
        );
    }
}
