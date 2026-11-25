package cl.induztek.ms_protocolos.config;

import cl.induztek.ms_protocolos.client.UsuariosClient;
import feign.FeignException;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Configuration
class SecurityConfig {
    @Bean
    SecurityFilterChain seguridad(HttpSecurity http, UsuariosClient usuarios) {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(errors -> errors.authenticationEntryPoint((req, res, error) -> res.sendError(401)))
                .authorizeHttpRequests(rutas -> rutas
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/protocolos/dependencias/**").hasAuthority("JEFE_TECNICO")
                        .requestMatchers(HttpMethod.GET, "/api/protocolos/pendientes", "/api/protocolos/*/variacion").hasAuthority("FORMALIZACION")
                        .requestMatchers(HttpMethod.PATCH, "/api/protocolos/*/formalizar", "/api/protocolos/*/observar")
                        .hasAuthority("FORMALIZACION")
                        .requestMatchers(HttpMethod.GET, "/api/protocolos/mis-protocolos", "/api/protocolos/resumen-visita").hasAuthority("PERSONAL")
                        .requestMatchers(HttpMethod.PATCH, "/api/protocolos/*/cerrar", "/api/protocolos/*/anular").hasAuthority("PERSONAL")
                        .requestMatchers(HttpMethod.GET, "/api/protocolos/**").hasAnyAuthority("PERSONAL", "CLIENTE")
                        .requestMatchers(HttpMethod.POST, "/api/protocolos/*/acta").hasAuthority("ENTREGA_ACTA")
                        .requestMatchers(HttpMethod.POST, "/api/protocolos").hasAuthority("PERSONAL")
                        .requestMatchers(HttpMethod.PUT, "/api/protocolos/*").hasAuthority("PERSONAL")
                        .requestMatchers(HttpMethod.DELETE, "/api/protocolos/*").hasAuthority("PERSONAL")
                        .anyRequest().denyAll())
                .addFilterBefore(new SesionFilter(usuarios), AnonymousAuthenticationFilter.class)
                .build();
    }

    private static final class SesionFilter extends OncePerRequestFilter {
        private final UsuariosClient usuarios;

        private SesionFilter(UsuariosClient usuarios) {
            this.usuarios = usuarios;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws ServletException, IOException {
            String autorizacion = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (autorizacion == null || !autorizacion.startsWith("Bearer ")) {
                response.sendError(401);
                return;
            }
            UsuariosClient.Usuario perfil;
            try {
                perfil = usuarios.perfil(autorizacion);
            } catch (FeignException error) {
                int estado = error.status();
                response.sendError(estado == 401 || estado == 403 ? estado : 503);
                return;
            }
            if (perfil == null || !perfil.activo() || perfil.id() <= 0 || !(perfil.esCliente() || perfil.esPersonal())) {
                response.sendError(403);
                return;
            }
            var permiso = new SimpleGrantedAuthority(perfil.esCliente() ? "CLIENTE" : "PERSONAL");
            var permisos = new java.util.ArrayList<SimpleGrantedAuthority>();
            permisos.add(permiso);
            if (perfil.rolId() == 3) {
                permisos.add(new SimpleGrantedAuthority("JEFE_TECNICO"));
            }
            if (perfil.rolId() == 2 || perfil.rolId() == 3) {
                permisos.add(new SimpleGrantedAuthority("FORMALIZACION"));
            }
            if (perfil.rolId() == 1 || perfil.rolId() == 2) {
                permisos.add(new SimpleGrantedAuthority("ENTREGA_ACTA"));
            }
            var contexto = SecurityContextHolder.createEmptyContext();
            contexto.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(perfil, null, permisos));
            SecurityContextHolder.setContext(contexto);
            chain.doFilter(request, response);
        }
    }
}
