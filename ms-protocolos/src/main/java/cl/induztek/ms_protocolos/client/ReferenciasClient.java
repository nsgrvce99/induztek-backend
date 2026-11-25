package cl.induztek.ms_protocolos.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

public interface ReferenciasClient {
    @FeignClient(name = "ms-equipos")
    interface Equipos {
        @GetMapping("/api/equipos/dependencias/clientes/{id}")
        java.util.List<Long> idsDelCliente(@RequestHeader(HttpHeaders.AUTHORIZATION) String autorizacion,
                                           @PathVariable("id") long id);

        @GetMapping("/api/equipos/{id}/referencia")
        Equipo consultar(@RequestHeader(HttpHeaders.AUTHORIZATION) String autorizacion, @PathVariable("id") long id);
    }

    @FeignClient(name = "ms-instrumentos")
    interface Instrumentos {
        @GetMapping("/api/instrumentos/{id}")
        Instrumento consultar(@RequestHeader(HttpHeaders.AUTHORIZATION) String autorizacion, @PathVariable("id") long id);
    }

    record Equipo(long id, long idCliente, String tag, String tipoEquipo, String instalacion, boolean archivado) {
        public Equipo(long id, long idCliente, String tag, String tipoEquipo, String instalacion) {
            this(id, idCliente, tag, tipoEquipo, instalacion, false);
        }
    }
    record Instrumento(long id, String calibracion) {}
}
