
package cl.induztek.ms_usuarios.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

class RutValidatorTest {

    private final RutValidator validator =
        new RutValidator();

    @Test
    @DisplayName("Validar RUT correcto con formato")
    void validarRutCorrecto() {

        boolean resultado =
            validator.isValid("12.345.678-5", null);

        assertTrue(resultado);
    }

    @Test
    @DisplayName("Validar RUT correcto sin formato")
    void validarRutSinFormato() {

        boolean resultado =
            validator.isValid("123456785", null);

        assertTrue(resultado);
    }

    @Test
    @DisplayName("Rechazar RUT con digito incorrecto")
    void rechazarRutIncorrecto() {

        boolean resultado =
            validator.isValid("12.345.678-0", null);

        assertFalse(resultado);
    }

    @Test
    @DisplayName("Rechazar RUT vacio")
    void rechazarRutVacio() {

        assertFalse(
            validator.isValid("", null)
        );
    }

    @Test
    @DisplayName("Rechazar RUT nulo")
    void rechazarRutNulo() {

        assertFalse(
            validator.isValid(null, null)
        );
    }

    @Test
    @DisplayName("Normalizar RUT eliminando formato")
    void normalizarRut() {

        String resultado =
            RutValidator.normalizar("12.345.678-5");

        assertEquals("123456785", resultado);
    }
}
