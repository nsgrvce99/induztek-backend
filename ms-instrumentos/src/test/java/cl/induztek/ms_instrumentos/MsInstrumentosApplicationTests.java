package cl.induztek.ms_instrumentos;

import cl.induztek.ms_instrumentos.client.UsuariosClient;
import cl.induztek.ms_instrumentos.service.Calibracion;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:instrumentos-test;MODE=MySQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "eureka.client.enabled=false"
})
class MsInstrumentosApplicationTests {
    @MockitoBean UsuariosClient usuarios;

    @Test
    void contexto() {}

    @Test
    void respetaLosLimitesDelAviso() {
        var hoy = LocalDate.of(2026, 11, 25);
        assertEquals(Calibracion.VENCIDA, Calibracion.evaluar(hoy.minusDays(1), hoy));
        assertEquals(Calibracion.POR_VENCER, Calibracion.evaluar(hoy, hoy));
        assertEquals(Calibracion.POR_VENCER, Calibracion.evaluar(hoy.plusDays(30), hoy));
        assertEquals(Calibracion.VIGENTE, Calibracion.evaluar(hoy.plusDays(31), hoy));
    }
}
