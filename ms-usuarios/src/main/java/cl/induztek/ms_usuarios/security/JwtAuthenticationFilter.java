package cl.induztek.ms_usuarios.security;

import cl.induztek.ms_usuarios.model.Usuario;
import cl.induztek.ms_usuarios.repository.UsuarioRepository;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication
    .UsernamePasswordAuthenticationToken;

import org.springframework.security.core.authority
    .SimpleGrantedAuthority;

import org.springframework.security.core.context
    .SecurityContextHolder;

import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthenticationFilter
        extends OncePerRequestFilter {

    private final JwtService jwtService;

    private final UsuarioRepository usuarioRepository;

    private final CredencialesService credencialesService;

    public JwtAuthenticationFilter(
        JwtService jwtService,
        UsuarioRepository usuarioRepository,
        CredencialesService credencialesService
    ) {

        this.jwtService = jwtService;

        this.usuarioRepository = usuarioRepository;

        this.credencialesService = credencialesService;
    }

    @Override
    protected boolean shouldNotFilter(
        HttpServletRequest request
    ) {

        return request.getServletPath()
            .equals("/api/auth/login");
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        String header =
            request.getHeader("Authorization");

        if (header == null
                || !header.startsWith("Bearer ")) {

            filterChain.doFilter(request, response);

            return;
        }

        String token = header.substring(7);

        try {

            Claims claims =
                jwtService.obtenerClaims(token);

            Long idUsuario =
                Long.valueOf(claims.getSubject());

            Usuario usuario = usuarioRepository
                .findById(idUsuario)
                .orElse(null);

            if (usuario == null || !usuario.isActivo()) {

                rechazar(response);

                return;
            }

            boolean temporalActual =
                credencialesService.requiereCambio(
                    usuario.getContrasena()
                );

            boolean temporalToken =
                Boolean.TRUE.equals(
                    claims.get(
                        "requiereCambioContrasena",
                        Boolean.class
                    )
                );

            // Invalida el token cuando cambia
            // el estado de la contraseña temporal.

            if (temporalActual != temporalToken) {

                rechazar(response);

                return;
            }

            String rolActual =
                usuario.getRol().getCodigo();

            if (!rolActual.equals(
                claims.get("rol", String.class)
            )) {

                rechazar(response);

                return;
            }

            String autoridad = temporalActual

                ? "CAMBIO_CONTRASENA"

                : "ROLE_" + rolActual;

            var permisos = List.of(
                new SimpleGrantedAuthority(autoridad)
            );

            var autenticacion =
                new UsernamePasswordAuthenticationToken(
                    usuario.getId().toString(),
                    null,
                    permisos
                );

            SecurityContextHolder.getContext()
                .setAuthentication(autenticacion);

        } catch (
            JwtException | IllegalArgumentException ex
        ) {

            SecurityContextHolder.clearContext();

            rechazar(response);

            return;
        }

        filterChain.doFilter(request, response);
    }

    private void rechazar(
        HttpServletResponse response
    ) throws IOException {

        response.setStatus(
            HttpServletResponse.SC_UNAUTHORIZED
        );

        response.setContentType(
            "application/json;charset=UTF-8"
        );

        response.getWriter().write("""
            {
                "status": 401,
                "mensaje": "Token inválido o expirado"
            }
            """);
    }
}