package cl.induztek.comun;

import feign.FeignException;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.server.ResponseStatusException;

import java.util.function.BooleanSupplier;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@FeignClient(name = "ms-protocolos", contextId = "dependenciasProtocolos")
public interface DependenciasProtocolos {
    @GetMapping("/api/protocolos/dependencias/equipos/{id}")
    boolean equipoFormalizado(@RequestHeader(HttpHeaders.AUTHORIZATION) String token, @PathVariable("id") long id);

    @GetMapping("/api/protocolos/dependencias/clientes/{id}")
    boolean clienteFormalizado(@RequestHeader(HttpHeaders.AUTHORIZATION) String token, @PathVariable("id") long id);

    @GetMapping("/api/protocolos/dependencias/instrumentos/{id}")
    boolean instrumentoEnUso(@RequestHeader(HttpHeaders.AUTHORIZATION) String token, @PathVariable("id") long id);

    static void verificar(BooleanSupplier dependencia, String mensaje) {
        try {
            if (dependencia.getAsBoolean()) throw new ResponseStatusException(CONFLICT, mensaje);
        } catch (FeignException error) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "No se pudieron comprobar las dependencias", error);
        }
    }
}