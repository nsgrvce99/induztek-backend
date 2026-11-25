package cl.induztek.ms_equipos.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "ms-usuarios")
public interface UsuariosClient {
    @GetMapping("/api/usuarios/me")
    Usuario perfil(@RequestHeader(HttpHeaders.AUTHORIZATION) String autorizacion);

    @GetMapping("/api/usuarios/{id}")
    Usuario buscar(@RequestHeader(HttpHeaders.AUTHORIZATION) String autorizacion,
                   @PathVariable("id") long id);

    record Usuario(long id, long rolId, boolean activo, boolean archivado) {
        public Usuario(long id, long rolId, boolean activo) {
            this(id, rolId, activo, false);
        }
        public boolean esCliente() {
            return rolId == 4;
        }

        public boolean esPersonal() {
            return rolId >= 1 && rolId <= 3;
        }
    }
}
