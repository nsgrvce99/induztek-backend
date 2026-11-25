package cl.induztek.api_gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.context.ApplicationContext;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = "eureka.client.enabled=false")
class ApiGatewayApplicationTests {
    @Autowired ApplicationContext contexto;
    @Autowired RouteLocator rutas;

    @Test
    void cargaSoloLosDestinosDeclarados() {
        var destinos = rutas.getRoutes().map(Route::getId).sort().collectList().block(Duration.ofSeconds(5));
        assertEquals(java.util.List.of("ms-equipos", "ms-instrumentos", "ms-protocolos", "ms-usuarios"), destinos);
    }

    @Test
    void servicioAusenteDevuelve503YRutaInternaQuedaBloqueada() {
        var api = WebTestClient.bindToApplicationContext(contexto).build();
        api.get().uri("/api/equipos/1").exchange().expectStatus().isEqualTo(503);
        api.get().uri("/interno/protocolos").exchange().expectStatus().isUnauthorized();
    }
}
