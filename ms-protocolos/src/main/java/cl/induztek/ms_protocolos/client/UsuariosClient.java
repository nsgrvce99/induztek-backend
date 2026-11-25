package cl.induztek.ms_protocolos.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "ms-usuarios")
public interface UsuariosClient {
    @GetMapping("/api/usuarios/me")
    Usuario perfil(@RequestHeader(HttpHeaders.AUTHORIZATION) String autorizacion);

    @GetMapping("/api/usuarios/{id}")
    Titular titular(@RequestHeader(HttpHeaders.AUTHORIZATION) String autorizacion, @PathVariable("id") long id);

    record Titular(long id, long rolId, String nombre, String rut, boolean archivado) {
        public Titular(long id, long rolId, String nombre, String rut) {
            this(id, rolId, nombre, rut, false);
        }
    }

    record Usuario(long id, long rolId, boolean activo) {
        public boolean esCliente() {
            return rolId == 4;
        }

        public boolean esPersonal() {
            return rolId >= 1 && rolId <= 3;
        }
    }
}
