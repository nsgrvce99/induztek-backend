
package cl.induztek.ms_usuarios.security;

import cl.induztek.ms_usuarios.model.Rol;
import cl.induztek.ms_usuarios.model.Usuario;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.ExpiredJwtException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import java.util.Base64;
import java.security.SecureRandom;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    private Usuario usuario;

    private String clavePrueba;

    @BeforeEach
    void configurar() {

        byte[] bytes = new byte[32];

        new SecureRandom().nextBytes(bytes);

        clavePrueba = Base64.getEncoder()
            .encodeToString(bytes);

        jwtService = new JwtService(
            clavePrueba,
            3600000L
        );

        Rol rol = new Rol();

        rol.setId(3L);
        rol.setCodigo("JEFE_TECNICO");
        rol.setNombre("Jefe técnico");

        usuario = new Usuario();

        usuario.setId(4L);
        usuario.setNombre("Jefe Demo");
        usuario.setCorreo("jefe@ejemplo.com");
        usuario.setRol(rol);
        usuario.setActivo(true);
    }

    // PRUEBA 1: GENERAR TOKEN

    @Test
    @DisplayName("Generar token JWT")
    void generarTokenCorrectamente() {

        String token =
            jwtService.generarToken(usuario, false);

        assertNotNull(token);
        assertFalse(token.isBlank());

        assertEquals(
            3,
            token.split("\\.").length
        );
    }

    // PRUEBA 2: OBTENER ID

    @Test
    @DisplayName("Obtener ID de usuario desde JWT")
    void obtenerIdUsuario() {

        String token =
            jwtService.generarToken(usuario, false);

        Long id =
            jwtService.obtenerIdUsuario(token);

        assertEquals(4L, id);
    }

    // PRUEBA 3: VERIFICAR ROL

    @Test
    @DisplayName("Verificar rol y emisor del JWT")
    void verificarRolDelToken() {

        String token =
            jwtService.generarToken(usuario, false);

        Claims claims =
            jwtService.obtenerClaims(token);

        assertEquals(
            "JEFE_TECNICO",
            claims.get("rol", String.class)
        );

        assertEquals(
            "induztek",
            claims.getIssuer()
        );
    }

    // PRUEBA 4: TOKEN TEMPORAL

    @Test
    @DisplayName("Verificar indicador de cambio de clave")
    void verificarTokenTemporal() {

        String token =
            jwtService.generarToken(usuario, true);

        Claims claims =
            jwtService.obtenerClaims(token);

        assertEquals(
            Boolean.TRUE,
            claims.get(
                "requiereCambioContrasena",
                Boolean.class
            )
        );
    }

    // PRUEBA 5: TOKEN INVALIDO

    @Test
    @DisplayName("Rechazar token con formato invalido")
    void rechazarTokenInvalido() {

        assertThrows(
            JwtException.class,
            () -> jwtService.obtenerClaims(
                "token-invalido"
            )
        );
    }

    // PRUEBA 6: TOKEN VENCIDO

    @Test
    @DisplayName("Rechazar token vencido")
    void rechazarTokenVencido() {

        JwtService jwtVencido =
            new JwtService(
                clavePrueba,
                -60000L
            );

        String token =
            jwtVencido.generarToken(usuario, false);

        assertThrows(
            ExpiredJwtException.class,
            () -> jwtService.obtenerClaims(token)
        );
    }

    // PRUEBA 7: FIRMA INCORRECTA

    @Test
    @DisplayName("Rechazar token firmado con otra clave")
    void rechazarFirmaIncorrecta() {

        byte[] otrosBytes = new byte[32];

        new SecureRandom().nextBytes(otrosBytes);

        String otraClave =
            Base64.getEncoder()
                .encodeToString(otrosBytes);

        JwtService otroJwtService =
            new JwtService(
                otraClave,
                3600000L
            );

        String token =
            otroJwtService.generarToken(
                usuario, false
            );

        assertThrows(
            JwtException.class,
            () -> jwtService.obtenerClaims(token)
        );
    }
}
