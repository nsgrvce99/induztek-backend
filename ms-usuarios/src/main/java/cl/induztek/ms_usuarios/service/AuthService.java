package cl.induztek.ms_usuarios.service;

import cl.induztek.ms_usuarios.dto.*;
import cl.induztek.ms_usuarios.model.Usuario;
import cl.induztek.ms_usuarios.repository.UsuarioRepository;
import cl.induztek.ms_usuarios.security.JwtService;
import cl.induztek.ms_usuarios.security.CredencialesService;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;

    private final JwtService jwtService;

    private final CredencialesService credencialesService;

    public AuthService(
        UsuarioRepository usuarioRepository,
        JwtService jwtService,
        CredencialesService credencialesService
    ) {

        this.usuarioRepository = usuarioRepository;

        this.jwtService = jwtService;

        this.credencialesService = credencialesService;
    }

    @Transactional(readOnly = true)
    public LoginResponse autenticar(
        LoginRequest request
    ) {

        String correo = request.correo()
                .trim()
                .toLowerCase(Locale.ROOT);

        Usuario usuario = usuarioRepository
            .findByCorreoAndActivoTrue(correo)

            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Credenciales inválidas o usuario inactivo"
                )
            );

        boolean valida =
            credencialesService.verificar(
                request.contrasena(),
                usuario.getContrasena()
            );

        if (!valida) {

            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Credenciales inválidas o usuario inactivo"
            );
        }

        boolean requiereCambio =
            credencialesService.requiereCambio(
                usuario.getContrasena()
            );

        String token = jwtService.generarToken(
            usuario,
            requiereCambio
        );

        return new LoginResponse(

            token,

            "Bearer",

            usuario.getId(),

            usuario.getRol().getId(),

            usuario.getRol().getNombre(),

            requiereCambio,

            UsuarioResponse.desdeEntidad(usuario)
        );
    }
}