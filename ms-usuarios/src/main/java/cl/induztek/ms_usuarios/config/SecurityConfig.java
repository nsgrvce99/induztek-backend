package cl.induztek.ms_usuarios.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Profile("local")
    public UserDetailsService usuariosDePrueba(
        @Value("${DEV_ADMIN_PASSWORD}") String clave,
        PasswordEncoder encoder
    ) {

        var administrador = User
                .withUsername("admin-local")
                .password(encoder.encode(clave))
                .roles("JEFE_TECNICO")
                .build();

        return new InMemoryUserDetailsManager(administrador);
    }

    @Bean
    @Profile("local")
    public SecurityFilterChain seguridadLocal(
        HttpSecurity http
    ) throws Exception {

        http
            .csrf(csrf -> csrf.disable())

            .sessionManagement(session -> session
                .sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            )

            .authorizeHttpRequests(auth -> auth
                .anyRequest().hasRole("JEFE_TECNICO")
            )

            .httpBasic(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    @Profile("!local")
    public SecurityFilterChain bloquearAcceso(
        HttpSecurity http
    ) throws Exception {

        http.authorizeHttpRequests(auth ->
            auth.anyRequest().denyAll()
        );

        return http.build();
    }
}