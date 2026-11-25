package cl.induztek.api_gateway;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
class GatewaySecurityConfig {
    @Bean
    SecurityWebFilterChain seguridad(ServerHttpSecurity http) {
        return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .authorizeExchange(rutas -> rutas
                        .pathMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                        // El servicio de destino autentica el JWT y decide los permisos.
                        .pathMatchers("/api/auth/**", "/api/usuarios/**", "/api/clientes/**", "/api/roles/**",
                                "/api/equipos/**", "/api/instalaciones/**", "/api/instrumentos/**",
                                "/api/protocolos/**").permitAll()
                        .anyExchange().denyAll())
                .build();
    }
}
