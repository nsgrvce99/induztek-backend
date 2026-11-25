
package cl.induztek.ms_usuarios.service;

import cl.induztek.ms_usuarios.dto.*;
import cl.induztek.ms_usuarios.model.Rol;
import cl.induztek.ms_usuarios.model.Usuario;
import cl.induztek.ms_usuarios.repository.RolRepository;
import cl.induztek.ms_usuarios.repository.UsuarioRepository;
import cl.induztek.ms_usuarios.validation.RutValidator;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.security.SecureRandom;
import java.util.Base64;
import java.nio.charset.StandardCharsets;

@Service
@Transactional
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    private static final long ROL_CLIENTE = 4L;
    private static final String TEMPORAL = "{temporal}";

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ==========================================
    // BUSCAR USUARIO
    // ==========================================

    @Transactional(readOnly = true)
    public UsuarioResponse buscarPorId(Long id) {

        return UsuarioResponse.desdeEntidad(
            obtenerUsuario(id)
        );
    }

    private Usuario obtenerUsuario(Long id) {

        return usuarioRepository.findById(id)
                .orElseThrow(() ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Usuario no encontrado"
                    )
                );
    }

    // ==========================================
    // OBTENER ROL
    // ==========================================

    private Rol obtenerRol(Long id) {

        return rolRepository.findById(id)
                .orElseThrow(() ->
                    new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Rol no válido"
                    )
                );
    }

    // ==========================================
    // VALIDAR RUT Y CORREO DUPLICADOS
    // ==========================================

    private void validarDuplicados(
        String rut,
        String correo,
        Long id
    ) {

        boolean rutExiste = id == null
                ? usuarioRepository.existsByRut(rut)
                : usuarioRepository.existsByRutAndIdNot(rut, id);

        if (rutExiste) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "El RUT ya está registrado"
            );
        }

        boolean correoExiste = id == null
                ? usuarioRepository.existsByCorreo(correo)
                : usuarioRepository.existsByCorreoAndIdNot(
                    correo, id
                );

        if (correoExiste) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "El correo ya está registrado"
            );
        }
    }

    private String normalizarCorreo(String correo) {

        return correo.trim().toLowerCase(Locale.ROOT);
    }

    // ==========================================
    // VALIDAR ROLES
    // ==========================================

    private void validarPersonal(Long rolId) {

        if (rolId == null || rolId < 1 || rolId > 3) {

            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El rol debe corresponder al personal interno"
            );
        }
    }

    private void validarCliente(Usuario usuario) {

        if (usuario.getRol().getId() != ROL_CLIENTE) {

            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El usuario no corresponde a un cliente"
            );
        }
    }

    // ==========================================
    // VALIDAR PAGINACION
    // ==========================================

    private void validarPaginacion(
        int limit,
        int offset
    ) {

        if (limit < 1 || limit > 100 || offset < 0) {

            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Limit debe estar entre 1 y 100; "
                    + "offset no puede ser negativo"
            );
        }
    }

    // ==========================================
    // VALIDAR LIMITE DE BCRYPT
    // ==========================================

    private void validarLongitudContrasena(
        String contrasena
    ) {

        if (contrasena == null ||
            contrasena.getBytes(
                StandardCharsets.UTF_8
            ).length > 72) {

            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La contraseña no puede superar los 72 bytes"
            );
        }
    }

    // ==========================================
    // GENERAR CONTRASENA TEMPORAL
    // ==========================================

    private String generarContrasenaTemporal() {

        byte[] bytes = new byte[12];

        new SecureRandom().nextBytes(bytes);

        return "A!" + Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    // ==========================================
    // REGISTRAR CLIENTE
    // ==========================================

    public UsuarioResponse registrarCliente(
        ClienteRequest request
    ) {

        String rut =
            RutValidator.normalizar(request.rut());

        String correo =
            normalizarCorreo(request.correo());

        validarDuplicados(rut, correo, null);

        Usuario usuario = new Usuario();

        usuario.setNombre(request.nombre().trim());
        usuario.setRut(rut);
        usuario.setCorreo(correo);
        usuario.setGiro(request.giro());

        usuario.setRol(obtenerRol(ROL_CLIENTE));

        usuario.setContrasena(
            passwordEncoder.encode(
                UUID.randomUUID().toString()
            )
        );

        usuario.setActivo(false);

        Usuario guardado =
            usuarioRepository.save(usuario);

        return UsuarioResponse.desdeEntidad(guardado);
    }

    // ==========================================
    // REGISTRAR PERSONAL INTERNO
    // ==========================================

    public PersonalCreadoResponse registrarPersonal(
        PersonalRequest request
    ) {

        validarPersonal(request.rolId());

        String rut =
            RutValidator.normalizar(request.rut());

        String correo =
            normalizarCorreo(request.correo());

        validarDuplicados(rut, correo, null);

        Usuario usuario = new Usuario();

        usuario.setNombre(request.nombre().trim());
        usuario.setRut(rut);
        usuario.setCorreo(correo);

        usuario.setRol(
            obtenerRol(request.rolId())
        );

        String claveTemporal =
            generarContrasenaTemporal();

        usuario.setContrasena(
            TEMPORAL +
            passwordEncoder.encode(claveTemporal)
        );

        usuario.setActivo(true);

        Usuario guardado =
            usuarioRepository.save(usuario);

        return new PersonalCreadoResponse(
            UsuarioResponse.desdeEntidad(guardado),
            claveTemporal
        );
    }

    // ==========================================
    // LISTAR CLIENTES
    // ==========================================

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listarClientes(
        String query,
        int limit,
        int offset
    ) {

        validarPaginacion(limit, offset);

        String filtro = query.trim();

        String filtroRut = filtro
                .replace(".", "")
                .replace("-", "");

        if (filtroRut.isBlank()) {
            filtroRut = filtro;
        }

        return usuarioRepository
                .buscarClientes(
                    filtro,
                    filtroRut,
                    limit,
                    offset
                )
                .stream()
                .map(UsuarioResponse::desdeEntidad)
                .toList();
    }

    // ==========================================
    // LISTAR PERSONAL INTERNO
    // ==========================================

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listarPersonal(
        String query,
        int limit,
        int offset
    ) {

        validarPaginacion(limit, offset);

        String filtro = query.trim();

        String filtroRut = filtro
                .replace(".", "")
                .replace("-", "");

        if (filtroRut.isBlank()) {
            filtroRut = filtro;
        }

        return usuarioRepository
                .buscarPersonal(
                    filtro,
                    filtroRut,
                    limit,
                    offset
                )
                .stream()
                .map(UsuarioResponse::desdeEntidad)
                .toList();
    }

    // ==========================================
    // ACTUALIZAR CLIENTE
    // ==========================================

    public UsuarioResponse actualizarCliente(
        Long id,
        ClienteRequest request
    ) {

        Usuario usuario = obtenerUsuario(id);

        validarCliente(usuario);

        String rut =
            RutValidator.normalizar(request.rut());

        String correo =
            normalizarCorreo(request.correo());

        validarDuplicados(rut, correo, id);

        usuario.setNombre(request.nombre().trim());
        usuario.setRut(rut);
        usuario.setCorreo(correo);
        usuario.setGiro(request.giro());

        return UsuarioResponse.desdeEntidad(
            usuarioRepository.save(usuario)
        );
    }

    // ==========================================
    // ACTUALIZAR PERSONAL INTERNO
    // ==========================================

    public UsuarioResponse actualizarPersonal(
        Long id,
        PersonalRequest request
    ) {

        Usuario usuario = obtenerUsuario(id);

        validarPersonal(usuario.getRol().getId());
        validarPersonal(request.rolId());

        String rut =
            RutValidator.normalizar(request.rut());

        String correo =
            normalizarCorreo(request.correo());

        validarDuplicados(rut, correo, id);

        usuario.setNombre(request.nombre().trim());
        usuario.setRut(rut);
        usuario.setCorreo(correo);

        usuario.setRol(
            obtenerRol(request.rolId())
        );

        return UsuarioResponse.desdeEntidad(
            usuarioRepository.save(usuario)
        );
    }

    // ==========================================
    // ACTIVAR CLIENTE
    // ==========================================

    public UsuarioResponse activarCliente(
        Long id,
        ActivacionRequest request
    ) {

        Usuario usuario = obtenerUsuario(id);

        validarCliente(usuario);

        if (usuario.isActivo()) {

            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "El cliente ya se encuentra activo"
            );
        }

        validarLongitudContrasena(
            request.contrasena()
        );

        usuario.setContrasena(
            TEMPORAL + passwordEncoder.encode(
                request.contrasena()
            )
        );

        usuario.setActivo(true);

        return UsuarioResponse.desdeEntidad(
            usuarioRepository.save(usuario)
        );
    }

    // ==========================================
    // ACTUALIZAR FOTO
    // ==========================================

    public UsuarioResponse actualizarFoto(
        Long id,
        FotoRequest request
    ) {

        Usuario usuario = obtenerUsuario(id);

        usuario.setFotoUri(request.fotoUri());

        return UsuarioResponse.desdeEntidad(
            usuarioRepository.save(usuario)
        );
    }

    // ==========================================
    // CAMBIAR CONTRASENA
    // ==========================================

    public void cambiarContrasena(
        Long id,
        CambiarContrasenaRequest request
    ) {

        Usuario usuario = obtenerUsuario(id);

        // Verificar que la cuenta esté activa.

        if (!usuario.isActivo()) {

            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "El usuario se encuentra inactivo"
            );
        }

        // Validar el límite de BCrypt.

        validarLongitudContrasena(request.actual());
        validarLongitudContrasena(request.nueva());

        // Obtener el hash almacenado.

        String hash = usuario.getContrasena();

        // Retirar el prefijo si es temporal.

        if (hash.startsWith(TEMPORAL)) {

            hash = hash.substring(TEMPORAL.length());
        }

        // Verificar contraseña actual.

        if (!passwordEncoder.matches(
                request.actual(), hash
        )) {

            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La contraseña actual es incorrecta"
            );
        }

        // Verificar confirmación.

        if (!request.nueva().equals(
                request.confirmar()
        )) {

            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Las contraseñas no coinciden"
            );
        }

        // Evitar reutilizar la contraseña actual.

        if (passwordEncoder.matches(
                request.nueva(), hash
        )) {

            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La nueva contraseña debe ser diferente"
            );
        }

        // Guardar la nueva contraseña con BCrypt.
        // Ya no tendrá el prefijo {temporal}.

        usuario.setContrasena(
            passwordEncoder.encode(request.nueva())
        );

        usuarioRepository.save(usuario);
    }

    // ==========================================
    // ELIMINAR CLIENTE
    // ==========================================

    public void eliminarCliente(Long id) {

        Usuario usuario = obtenerUsuario(id);

        validarCliente(usuario);

        throw new ResponseStatusException(
            HttpStatus.CONFLICT,
            "Eliminación pendiente de verificar equipos, "
                + "instalaciones y protocolos asociados"
        );
    }

    // ==========================================
    // ELIMINAR PERSONAL
    // ==========================================

    public void eliminarPersonal(Long id) {

        Usuario usuario = obtenerUsuario(id);

        validarPersonal(usuario.getRol().getId());

        throw new ResponseStatusException(
            HttpStatus.CONFLICT,
            "Eliminación pendiente de verificar "
                + "protocolos asociados al usuario"
        );
    }
}
