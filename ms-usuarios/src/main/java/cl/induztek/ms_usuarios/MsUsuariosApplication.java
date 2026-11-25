package cl.induztek.ms_usuarios;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@org.springframework.context.annotation.Import(cl.induztek.comun.CoordinacionReferencias.class)
@org.springframework.cloud.openfeign.EnableFeignClients(basePackages = {"cl.induztek.ms_usuarios", "cl.induztek.comun"})
@SpringBootApplication
public class MsUsuariosApplication {

    public static void main(String[] args) {
        SpringApplication.run(
            MsUsuariosApplication.class, args
        );
    }
}
