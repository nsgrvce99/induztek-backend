package cl.induztek.ms_usuarios.config;

import cl.induztek.ms_usuarios.repository.UsuarioRepository;
import cl.induztek.ms_usuarios.security.JwtService;
import cl.induztek.ms_usuarios.security.JwtAuthenticationFilter;
import cl.induztek.ms_usuarios.security.CredencialesService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.http.HttpMethod;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.SecurityFilterChain;

import org.springframework.security.web.authentication
    .UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(

        HttpSecurity http,

        JwtService jwtService,

        UsuarioRepository usuarioRepository,

        CredencialesService credencialesService

    ) throws Exception {

        var jwtFilter = new JwtAuthenticationFilter(
            jwtService,
            usuarioRepository,
            credencialesService
        );

        http

            .csrf(csrf -> csrf.disable())

            .httpBasic(basic -> basic.disable())

            .formLogin(form -> form.disable())

            .sessionManagement(session -> session

                .sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            )

            .exceptionHandling(ex -> ex

                .authenticationEntryPoint(
                    (request, response, error) -> {

                        response.setStatus(401);

                        response.setContentType(
                            "application/json;charset=UTF-8"
                        );

                        response.getWriter().write("""
                            {
                                "status": 401,
                                "mensaje": "Debe autenticarse"
                            }
                            """);
                    }
                )

                .accessDeniedHandler(
                    (request, response, error) -> {

                        response.setStatus(403);

                        response.setContentType(
                            "application/json;charset=UTF-8"
                        );

                        response.getWriter().write("""
                            {
                                "status": 403,
                                "mensaje": "No tiene permisos"
                            }
                            """);
                    }
                )
            )

            .authorizeHttpRequests(auth -> auth

                // LOGIN PUBLICO

                .requestMatchers(
                    HttpMethod.POST,
                    "/api/auth/login"
                ).permitAll()

                // CAMBIO DE CONTRASENA PROPIA

                .requestMatchers(
                    HttpMethod.PATCH,
                    "/api/usuarios/me/contrasena"
                ).hasAnyAuthority(
                    "CAMBIO_CONTRASENA",
                    "ROLE_TECNICO",
                    "ROLE_INGENIERO",
                    "ROLE_JEFE_TECNICO",
                    "ROLE_CLIENTE"
                )

                // PERFIL PROPIO

                .requestMatchers(
                    HttpMethod.GET,
                    "/api/usuarios/me"
                ).hasAnyRole(
                    "TECNICO",
                    "INGENIERO",
                    "JEFE_TECNICO",
                    "CLIENTE"
                )

                // FOTO PROPIA

                .requestMatchers(
                    HttpMethod.PATCH,
                    "/api/usuarios/me/foto"
                ).hasAnyRole(
                    "TECNICO",
                    "INGENIERO",
                    "JEFE_TECNICO",
                    "CLIENTE"
                )

                // GESTION PERSONAL INTERNO

                .requestMatchers(
                    "/api/usuarios/personal"
                ).hasRole("JEFE_TECNICO")

                // ACTIVACION CLIENTES

                .requestMatchers(
                    HttpMethod.PATCH,
                    "/api/clientes/*/activar"
                ).hasRole("JEFE_TECNICO")

                // ELIMINAR CLIENTES

                .requestMatchers(
                    HttpMethod.DELETE,
                    "/api/clientes/*"
                ).hasAnyRole(
                    "TECNICO",
                    "INGENIERO",
                    "JEFE_TECNICO"
                )

                // CONSULTAR Y REGISTRAR CLIENTES

                .requestMatchers(
                    HttpMethod.GET,
                    "/api/clientes"
                ).hasAnyRole(
                    "TECNICO",
                    "INGENIERO",
                    "JEFE_TECNICO"
                )

                .requestMatchers(
                    HttpMethod.POST,
                    "/api/clientes"
                ).hasAnyRole(
                    "TECNICO",
                    "INGENIERO",
                    "JEFE_TECNICO"
                )

                // ACTUALIZAR CLIENTES

                .requestMatchers(
                    HttpMethod.PUT,
                    "/api/clientes/*"
                ).hasAnyRole(
                    "TECNICO",
                    "INGENIERO",
                    "JEFE_TECNICO"
                )

                // CONSULTAR ROLES

                .requestMatchers(
                    HttpMethod.GET,
                    "/api/roles"
                ).hasRole("JEFE_TECNICO")

                // CONSULTAR USUARIOS POR ID

                .requestMatchers(
                    HttpMethod.GET,
                    "/api/usuarios/*"
                ).hasAnyRole(
                    "TECNICO",
                    "INGENIERO",
                    "JEFE_TECNICO"
                )

                // ACTUALIZAR O ELIMINAR PERSONAL

                .requestMatchers(
                    HttpMethod.PUT,
                    "/api/usuarios/*"
                ).hasRole("JEFE_TECNICO")

                .requestMatchers(
                    HttpMethod.DELETE,
                    "/api/usuarios/*"
                ).hasRole("JEFE_TECNICO")

                // DENEGAR CUALQUIER OTRA RUTA

                .anyRequest().denyAll()
            )

            .addFilterBefore(
                jwtFilter,
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }
}