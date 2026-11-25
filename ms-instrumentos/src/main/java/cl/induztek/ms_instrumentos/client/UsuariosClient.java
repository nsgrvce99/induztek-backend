package cl.induztek.ms_instrumentos.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "ms-usuarios")
public interface UsuariosClient {
    @GetMapping("/api/usuarios/me")
    Usuario perfil(@RequestHeader(HttpHeaders.AUTHORIZATION) String autorizacion);

    record Usuario(long id, long rolId, boolean activo) {
        public boolean esCliente() {
            return rolId == 4;
        }

        public boolean esPersonal() {
            return rolId >= 1 && rolId <= 3;
        }
    }
}
