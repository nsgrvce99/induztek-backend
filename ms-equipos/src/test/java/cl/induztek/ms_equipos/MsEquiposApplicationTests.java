package cl.induztek.ms_equipos;

import cl.induztek.ms_equipos.client.UsuariosClient;
import cl.induztek.ms_equipos.model.Equipo;
import cl.induztek.ms_equipos.model.Instalacion;
import cl.induztek.ms_equipos.repository.EquipoRepository;
import cl.induztek.ms_equipos.repository.InstalacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:equipos-test;MODE=MySQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "eureka.client.enabled=false"
})
class MsEquiposApplicationTests {
    @Autowired WebApplicationContext contexto;
    @Autowired EquipoRepository equipos;
    @Autowired InstalacionRepository instalaciones;
    @MockitoBean UsuariosClient usuarios;

    MockMvc api;

    @BeforeEach
    void preparar() {
        api = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
    }

    @Test
    void solicitaUnaSesion() throws Exception {
        api.perform(get("/api/instalaciones")).andExpect(status().isUnauthorized());
    }

    @Test
    void clienteSoloConsultaEquiposPropios() throws Exception {
        Instalacion planta = instalaciones.save(new Instalacion("Planta de prueba", 42L));
        Equipo equipo = equipos.save(new Equipo("TR-PRUEBA", "Transformador", "Rhona", "SERIE", planta));
        when(usuarios.perfil("Bearer cliente")).thenReturn(new UsuariosClient.Usuario(42, 4, true));
        when(usuarios.perfil("Bearer otro")).thenReturn(new UsuariosClient.Usuario(43, 4, true));

        String ruta = "/api/equipos/" + equipo.getId();
        api.perform(get(ruta).header(HttpHeaders.AUTHORIZATION, "Bearer cliente"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.idCliente").value(42));
        api.perform(get(ruta).header(HttpHeaders.AUTHORIZATION, "Bearer otro"))
                .andExpect(status().isNotFound());
        api.perform(get("/api/instalaciones").header(HttpHeaders.AUTHORIZATION, "Bearer cliente"))
                .andExpect(status().isForbidden());
    }

    @Test
    void tecnicoEliminaUnaPlantaVaciaPeroNoUnaConEquipos() throws Exception {
        when(usuarios.perfil("Bearer tecnico")).thenReturn(new UsuariosClient.Usuario(1, 1, true));
        var ocupada = instalaciones.save(new Instalacion("Planta ocupada", 42L));
        equipos.save(new Equipo("TR", "Transformador", "Rhona", "01", ocupada));
        var libre = instalaciones.save(new Instalacion("Planta libre", 42L));
        api.perform(delete("/api/instalaciones/" + ocupada.getId()).header(HttpHeaders.AUTHORIZATION, "Bearer tecnico"))
                .andExpect(status().isConflict());
        api.perform(delete("/api/instalaciones/" + libre.getId()).header(HttpHeaders.AUTHORIZATION, "Bearer tecnico"))
                .andExpect(status().isNoContent());
        api.perform(get("/api/instalaciones/" + libre.getId()).header(HttpHeaders.AUTHORIZATION, "Bearer tecnico"))
                .andExpect(status().isNotFound());
    }
}
