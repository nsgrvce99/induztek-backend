package cl.induztek.ms_usuarios;

import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:usuarios-test;MODE=MySQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "eureka.client.enabled=false",
    "jwt.secret=dGVzdC1pbmR1enRlay1jb250ZXh0by1tcy11c3Vhcmlvcy0wMQ=="
})
class MsUsuariosApplicationTests {

    @MockitoBean(name = "cargarUsuariosIniciales")
    ApplicationRunner cargaInicial;

    @Test
    void contextLoads() {
    }
}
