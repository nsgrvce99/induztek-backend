
package cl.induztek.ms_usuarios.service;

import cl.induztek.ms_usuarios.dto.*;
import cl.induztek.ms_usuarios.model.Rol;
import cl.induztek.ms_usuarios.model.Usuario;
import cl.induztek.ms_usuarios.repository.RolRepository;
import cl.induztek.ms_usuarios.repository.UsuarioRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private RolRepository rolRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioService usuarioService;

    private Usuario usuario;
    private Rol rolCliente;
    private ClienteRequest clienteRequest;

    @BeforeEach
    void configurar() {

        rolCliente = new Rol();

        rolCliente.setId(4L);
        rolCliente.setCodigo("CLIENTE");
        rolCliente.setNombre("Usuario cliente");

        usuario = new Usuario();

        usuario.setId(100L);
        usuario.setRut("123456785");
        usuario.setNombre("Empresa Demo");
        usuario.setCorreo("cliente@ejemplo.com");
        usuario.setRol(rolCliente);
        usuario.setActivo(false);
        usuario.setContrasena("hash_demo");

        clienteRequest = new ClienteRequest(
            "Empresa Demo",
            "12.345.678-5",
            "cliente@ejemplo.com",
            "Industrial"
        );
    }

    // =======================================
    // PRUEBA 1: REGISTRAR CLIENTE
    // =======================================

    @Test
    @DisplayName("Registrar cliente correctamente")
    void registrarClienteCorrectamente() {

        when(usuarioRepository.existsByRut(
            "123456785"
        )).thenReturn(false);

        when(usuarioRepository.existsByCorreo(
            "cliente@ejemplo.com"
        )).thenReturn(false);

        when(rolRepository.findById(4L))
            .thenReturn(Optional.of(rolCliente));

        when(passwordEncoder.encode(anyString()))
            .thenReturn("hash_demo");

        when(usuarioRepository.save(any(Usuario.class)))
            .thenAnswer(invocacion -> {

                Usuario guardado =
                    invocacion.getArgument(0);

                guardado.setId(100L);

                return guardado;
            });

        UsuarioResponse resultado =
            usuarioService.registrarCliente(
                clienteRequest
            );

        assertNotNull(resultado);
        assertEquals(100L, resultado.id());
        assertEquals(4L, resultado.rolId());
        assertEquals("123456785", resultado.rut());
        assertFalse(resultado.activo());

        verify(usuarioRepository, times(1))
            .save(any(Usuario.class));
    }

    // =======================================
    // PRUEBA 2: RUT DUPLICADO
    // =======================================

    @Test
    @DisplayName("Rechazar registro con RUT duplicado")
    void rechazarRutDuplicado() {

        when(usuarioRepository.existsByRut(
            "123456785"
        )).thenReturn(true);

        ResponseStatusException excepcion =
            assertThrows(
                ResponseStatusException.class,
                () -> usuarioService.registrarCliente(
                    clienteRequest
                )
            );

        assertEquals(
            HttpStatus.CONFLICT,
            excepcion.getStatusCode()
        );

        assertEquals(
            "El RUT ya está registrado",
            excepcion.getReason()
        );

        verify(usuarioRepository, never())
            .save(any(Usuario.class));
    }

    // =======================================
    // PRUEBA 3: CORREO DUPLICADO
    // =======================================

    @Test
    @DisplayName("Rechazar correo duplicado")
    void rechazarCorreoDuplicado() {

        when(usuarioRepository.existsByRut(
            "123456785"
        )).thenReturn(false);

        when(usuarioRepository.existsByCorreo(
            "cliente@ejemplo.com"
        )).thenReturn(true);

        ResponseStatusException excepcion =
            assertThrows(
                ResponseStatusException.class,
                () -> usuarioService.registrarCliente(
                    clienteRequest
                )
            );

        assertEquals(
            HttpStatus.CONFLICT,
            excepcion.getStatusCode()
        );

        assertEquals(
            "El correo ya está registrado",
            excepcion.getReason()
        );

        verify(usuarioRepository, never())
            .save(any(Usuario.class));
    }

    // =======================================
    // PRUEBA 4: BUSCAR USUARIO
    // =======================================

    @Test
    @DisplayName("Buscar usuario existente por ID")
    void buscarUsuarioExistente() {

        when(usuarioRepository.findById(100L))
            .thenReturn(Optional.of(usuario));

        UsuarioResponse resultado =
            usuarioService.buscarPorId(100L);

        assertEquals(100L, resultado.id());

        assertEquals(
            "Empresa Demo",
            resultado.nombre()
        );

        assertEquals(4L, resultado.rolId());
    }

    // =======================================
    // PRUEBA 5: USUARIO NO EXISTENTE
    // =======================================

    @Test
    @DisplayName("Buscar usuario que no existe")
    void buscarUsuarioInexistente() {

        when(usuarioRepository.findById(999L))
            .thenReturn(Optional.empty());

        ResponseStatusException excepcion =
            assertThrows(
                ResponseStatusException.class,
                () -> usuarioService.buscarPorId(999L)
            );

        assertEquals(
            HttpStatus.NOT_FOUND,
            excepcion.getStatusCode()
        );
    }

    // =======================================
    // PRUEBA 6: ACTIVAR CLIENTE
    // =======================================

    @Test
    @DisplayName("Activar cliente inactivo")
    void activarClienteCorrectamente() {

        ActivacionRequest request =
            new ActivacionRequest("Temporal!26");

        when(usuarioRepository.findById(100L))
            .thenReturn(Optional.of(usuario));

        when(passwordEncoder.encode("Induztek2026!"))
            .thenReturn("hash_temporal");

        when(usuarioRepository.save(usuario))
            .thenReturn(usuario);

        UsuarioResponse resultado =
            usuarioService.activarCliente(
                100L, request
            );

        assertTrue(resultado.activo());

        assertEquals(
            "{temporal}hash_temporal",
            usuario.getContrasena()
        );

        verify(usuarioRepository).save(usuario);
    }

    // =======================================
    // PRUEBA 7: CLIENTE YA ACTIVO
    // =======================================

    @Test
    @DisplayName("Rechazar activacion de cliente activo")
    void rechazarClienteYaActivo() {

        usuario.setActivo(true);

        when(usuarioRepository.findById(100L))
            .thenReturn(Optional.of(usuario));

        ActivacionRequest request =
            new ActivacionRequest("Temporal!26");

        ResponseStatusException excepcion =
            assertThrows(
                ResponseStatusException.class,
                () -> usuarioService.activarCliente(
                    100L, request
                )
            );

        assertEquals(
            HttpStatus.CONFLICT,
            excepcion.getStatusCode()
        );

        verify(usuarioRepository, never())
            .save(any(Usuario.class));
    }

    // =======================================
    // PRUEBA 8: LISTAR CLIENTES
    // =======================================

    @Test
    @DisplayName("Listar clientes con paginacion")
    void listarClientesCorrectamente() {

        when(usuarioRepository.buscarClientes(
            "", "", 20, 0
        )).thenReturn(List.of(usuario));

        List<UsuarioResponse> resultado =
            usuarioService.listarClientes(
                "", 20, 0
            );

        assertEquals(1, resultado.size());

        assertEquals(
            "Empresa Demo",
            resultado.get(0).nombre()
        );

        verify(usuarioRepository)
            .buscarClientes("", "", 20, 0);
    }

    // =======================================
    // PRUEBA 9: PAGINACION INVALIDA
    // =======================================

    @Test
    @DisplayName("Rechazar paginacion invalida")
    void rechazarPaginacionInvalida() {

        ResponseStatusException excepcion =
            assertThrows(
                ResponseStatusException.class,
                () -> usuarioService.listarClientes(
                    "", 0, -1
                )
            );

        assertEquals(
            HttpStatus.BAD_REQUEST,
            excepcion.getStatusCode()
        );

        verify(usuarioRepository, never())
            .buscarClientes(
                anyString(),
                anyString(),
                anyInt(),
                anyInt()
            );
    }

    // =======================================
    // PRUEBA 10: CAMBIAR CONTRASENA
    // =======================================

    @Test
    @DisplayName("Cambiar contrasena temporal")
    void cambiarContrasenaCorrectamente() {

        usuario.setActivo(true);

        usuario.setContrasena(
            "{temporal}hash_anterior"
        );

        CambiarContrasenaRequest request =
            new CambiarContrasenaRequest(
                "Temporal!26",
                "NuevaClave!26",
                "NuevaClave!26"
            );

        when(usuarioRepository.findById(100L))
            .thenReturn(Optional.of(usuario));

        when(passwordEncoder.matches(
            "Temporal!26", "hash_anterior"
        )).thenReturn(true);

        when(passwordEncoder.matches(
            "NuevaClave!26", "hash_anterior"
        )).thenReturn(false);

        when(passwordEncoder.encode(
            "NuevaClave!26"
        )).thenReturn("hash_nuevo");

        usuarioService.cambiarContrasena(
            100L, request
        );

        assertEquals(
            "hash_nuevo",
            usuario.getContrasena()
        );

        assertFalse(
            usuario.getContrasena()
                .startsWith("{temporal}")
        );

        verify(usuarioRepository).save(usuario);
    }

    // =======================================
    // PRUEBA 11: CONTRASENA INCORRECTA
    // =======================================

    @Test
    @DisplayName("Rechazar contrasena actual incorrecta")
    void rechazarContrasenaIncorrecta() {

        usuario.setActivo(true);

        CambiarContrasenaRequest request =
            new CambiarContrasenaRequest(
                "Incorrecta!26",
                "NuevaClave!26",
                "NuevaClave!26"
            );

        when(usuarioRepository.findById(100L))
            .thenReturn(Optional.of(usuario));

        when(passwordEncoder.matches(
            "Incorrecta!26", "hash_demo"
        )).thenReturn(false);

        ResponseStatusException excepcion =
            assertThrows(
                ResponseStatusException.class,
                () -> usuarioService.cambiarContrasena(
                    100L, request
                )
            );

        assertEquals(
            HttpStatus.BAD_REQUEST,
            excepcion.getStatusCode()
        );

        verify(usuarioRepository, never())
            .save(any(Usuario.class));
    }

    // =======================================
    // PRUEBA 12: ELIMINACION PROTEGIDA
    // =======================================

    @Test
    @DisplayName("Archivar cliente y revocar su acceso")
    void archivarCliente() {
        when(usuarioRepository.findById(100L)).thenReturn(Optional.of(usuario));

        usuarioService.eliminarCliente(100L);

        assertFalse(usuario.isActivo());
        assertTrue(usuario.isArchivado());
        verify(usuarioRepository, never()).delete(any(Usuario.class));
    }
}
