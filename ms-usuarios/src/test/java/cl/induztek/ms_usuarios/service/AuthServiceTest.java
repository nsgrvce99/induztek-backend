
package cl.induztek.ms_usuarios.service;

import cl.induztek.ms_usuarios.dto.LoginRequest;
import cl.induztek.ms_usuarios.dto.LoginResponse;
import cl.induztek.ms_usuarios.model.Rol;
import cl.induztek.ms_usuarios.model.Usuario;
import cl.induztek.ms_usuarios.repository.UsuarioRepository;
import cl.induztek.ms_usuarios.security.JwtService;
import cl.induztek.ms_usuarios.security.CredencialesService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private CredencialesService credencialesService;

    @InjectMocks
    private AuthService authService;

    private Usuario usuario;

    @BeforeEach
    void configurar() {

        Rol rol = new Rol();

        rol.setId(3L);
        rol.setCodigo("JEFE_TECNICO");
        rol.setNombre("Jefe técnico");

        usuario = new Usuario();

        usuario.setId(4L);
        usuario.setNombre("Jefe de Prueba");
        usuario.setRut("123456785");
        usuario.setCorreo("jefe@ejemplo.com");
        usuario.setActivo(true);
        usuario.setRol(rol);
        usuario.setContrasena("hash_demo");
    }

    // PRUEBA 1: LOGIN CORRECTO

    @Test
    @DisplayName("Autenticar usuario correctamente")
    void autenticarCorrectamente() {

        LoginRequest request = new LoginRequest(
            "jefe@ejemplo.com",
            "ClaveDemo!26"
        );

        when(usuarioRepository.findByCorreoAndActivoTrue(
            "jefe@ejemplo.com"
        )).thenReturn(Optional.of(usuario));

        when(credencialesService.verificar(
            "ClaveDemo!26", "hash_demo"
        )).thenReturn(true);

        when(credencialesService.requiereCambio(
            "hash_demo"
        )).thenReturn(false);

        when(jwtService.generarToken(usuario, false))
            .thenReturn("token_jwt_demo");

        LoginResponse resultado =
            authService.autenticar(request);

        assertNotNull(resultado);

        assertEquals(
            "token_jwt_demo",
            resultado.token()
        );

        assertEquals("Bearer", resultado.tipoToken());
        assertEquals(4L, resultado.idUsuario());
        assertEquals(3L, resultado.rolId());

        assertFalse(
            resultado.requiereCambioContrasena()
        );

        verify(jwtService)
            .generarToken(usuario, false);
    }

    // PRUEBA 2: CONTRASENA INCORRECTA

    @Test
    @DisplayName("Rechazar credenciales incorrectas")
    void rechazarCredencialesIncorrectas() {

        LoginRequest request = new LoginRequest(
            "jefe@ejemplo.com",
            "Incorrecta"
        );

        when(usuarioRepository.findByCorreoAndActivoTrue(
            "jefe@ejemplo.com"
        )).thenReturn(Optional.of(usuario));

        when(credencialesService.verificar(
            "Incorrecta", "hash_demo"
        )).thenReturn(false);

        ResponseStatusException excepcion =
            assertThrows(
                ResponseStatusException.class,
                () -> authService.autenticar(request)
            );

        assertEquals(
            HttpStatus.UNAUTHORIZED,
            excepcion.getStatusCode()
        );

        verify(jwtService, never())
            .generarToken(
                any(Usuario.class),
                anyBoolean()
            );
    }

    // PRUEBA 3: USUARIO NO ENCONTRADO O INACTIVO

    @Test
    @DisplayName("Rechazar usuario inexistente o inactivo")
    void rechazarUsuarioInexistenteOInactivo() {

        LoginRequest request = new LoginRequest(
            "inactivo@ejemplo.com",
            "ClaveDemo!26"
        );

        when(usuarioRepository.findByCorreoAndActivoTrue(
            "inactivo@ejemplo.com"
        )).thenReturn(Optional.empty());

        ResponseStatusException excepcion =
            assertThrows(
                ResponseStatusException.class,
                () -> authService.autenticar(request)
            );

        assertEquals(
            HttpStatus.UNAUTHORIZED,
            excepcion.getStatusCode()
        );

        verifyNoInteractions(jwtService);
    }

    // PRUEBA 4: CONTRASENA TEMPORAL

    @Test
    @DisplayName("Detectar cambio obligatorio de contrasena")
    void detectarContrasenaTemporal() {

        usuario.setContrasena(
            "{temporal}hash_demo"
        );

        LoginRequest request = new LoginRequest(
            "jefe@ejemplo.com",
            "Temporal!26"
        );

        when(usuarioRepository.findByCorreoAndActivoTrue(
            "jefe@ejemplo.com"
        )).thenReturn(Optional.of(usuario));

        when(credencialesService.verificar(
            "Temporal!26",
            "{temporal}hash_demo"
        )).thenReturn(true);

        when(credencialesService.requiereCambio(
            "{temporal}hash_demo"
        )).thenReturn(true);

        when(jwtService.generarToken(usuario, true))
            .thenReturn("token_temporal_demo");

        LoginResponse resultado =
            authService.autenticar(request);

        assertTrue(
            resultado.requiereCambioContrasena()
        );

        assertEquals(
            "token_temporal_demo",
            resultado.token()
        );

        verify(jwtService)
            .generarToken(usuario, true);
    }
}

