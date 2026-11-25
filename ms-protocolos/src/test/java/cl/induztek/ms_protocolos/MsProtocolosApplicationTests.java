package cl.induztek.ms_protocolos;

import cl.induztek.ms_protocolos.model.Medicion;
import cl.induztek.ms_protocolos.model.Protocolo;
import cl.induztek.ms_protocolos.repository.MedicionRepository;
import cl.induztek.ms_protocolos.repository.ProtocoloRepository;
import cl.induztek.ms_protocolos.service.EvaluacionPrueba.Resultado;
import cl.induztek.ms_protocolos.service.EvaluacionPrueba.Tipo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import cl.induztek.ms_protocolos.client.ReferenciasClient;
import cl.induztek.ms_protocolos.client.UsuariosClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:protocolos-test;MODE=MySQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "eureka.client.enabled=false"
})
class MsProtocolosApplicationTests {
    @Autowired ProtocoloRepository protocolos;
    @Autowired MedicionRepository mediciones;
    @Autowired WebApplicationContext contexto;
    @MockitoBean UsuariosClient usuarios;
    @MockitoBean ReferenciasClient.Equipos equipos;
    @MockitoBean ReferenciasClient.Instrumentos instrumentos;

    @Test
    void registraDuenioVigenteYRestringeBorradoresAlPersonal() throws Exception {
        var api = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        when(usuarios.perfil("Bearer tecnico")).thenReturn(new UsuariosClient.Usuario(11L, 1L, true));
        when(usuarios.perfil("Bearer ingeniero")).thenReturn(new UsuariosClient.Usuario(12L, 2L, true));
        when(usuarios.perfil("Bearer cliente")).thenReturn(new UsuariosClient.Usuario(42L, 4L, true));
        when(equipos.consultar("Bearer tecnico", 8L)).thenReturn(new ReferenciasClient.Equipo(8L, 42L, "TR-DEMO", "Transformador", "Planta Demo"));
        when(equipos.consultar("Bearer cliente", 8L)).thenReturn(new ReferenciasClient.Equipo(8L, 42L, "TR-DEMO", "Transformador", "Planta Demo"));
        when(usuarios.titular("Bearer tecnico", 42L))
                .thenReturn(new UsuariosClient.Titular(42L, 4L, "Empresa de prueba", "1-9"));
        when(usuarios.titular("Bearer ingeniero", 42L))
                .thenReturn(new UsuariosClient.Titular(42L, 4L, "Empresa de prueba", "1-9"));
        when(equipos.consultar("Bearer ingeniero", 8L))
                .thenReturn(new ReferenciasClient.Equipo(8L, 42L, "TR-DEMO", "Transformador", "Planta Demo"));
        when(instrumentos.consultar("Bearer tecnico", 3L))
                .thenReturn(new ReferenciasClient.Instrumento(3L, "VIGENTE"));

        var creado = api.perform(post("/api/protocolos").header("Authorization", "Bearer tecnico")
                        .contentType("application/json").content("""
                                {"equipoId":8,"instrumentoId":3,"temperatura":20,"humedad":50,
                                 "tipoPrueba":"AISLACION","valores":{"lectura1Min":10,"lectura10Min":20}}
                                """))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.tecnicoId").value(11))
                .andExpect(jsonPath("$.estado").value("BORRADOR")).andReturn();

        api.perform(get("/api/protocolos/equipo/8").header("Authorization", "Bearer cliente"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(0));

        var id = com.jayway.jsonpath.JsonPath.read(creado.getResponse().getContentAsString(), "$.id");
        api.perform(patch("/api/protocolos/" + id + "/cerrar").header("Authorization", "Bearer tecnico"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("REGISTRADO_TERRENO"));
        api.perform(get("/api/protocolos/equipo/8").header("Authorization", "Bearer cliente"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1));

        api.perform(post("/api/protocolos/" + id + "/acta").header("Authorization", "Bearer tecnico"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.nombreReceptor").value("Empresa de prueba"));

        when(usuarios.titular("Bearer tecnico", 11L))
                .thenReturn(new UsuariosClient.Titular(11L, 1L, "Técnico de prueba", "6-K"));
        api.perform(get("/api/protocolos/" + id).header("Authorization", "Bearer tecnico"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.equipo.tag").value("TR-DEMO"))
                .andExpect(jsonPath("$.tecnico.id").value(11)).andExpect(jsonPath("$.acta.id").exists());
        api.perform(get("/api/protocolos/" + id).header("Authorization", "Bearer cliente"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.acta").doesNotExist());
        api.perform(get("/api/protocolos/" + id + "/variacion").header("Authorization", "Bearer tecnico"))
                .andExpect(status().isForbidden());
        api.perform(get("/api/protocolos/" + id + "/variacion").header("Authorization", "Bearer ingeniero"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.significativa").value(false));
        api.perform(patch("/api/protocolos/" + id + "/observar").header("Authorization", "Bearer ingeniero")
                        .contentType("application/json").content("{\"motivo\":\"Revisar aislamiento\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("OBSERVADO"));
        String correccion = """
                {"equipoId":8,"instrumentoId":3,"temperatura":23,"humedad":22.0,
                 "tipoPrueba":"AISLACION","valores":{"lectura1Min":10.00,"lectura10Min":21},
                 "textoIngresado":{"temperatura":"023","humedad":"22.0","lectura1Min":"10.00"}}
                """;
        api.perform(put("/api/protocolos/" + id).header("Authorization", "Bearer tecnico")
                        .contentType("application/json").content(correccion))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("REGISTRADO_TERRENO"));

        api.perform(get("/api/protocolos/" + id).header("Authorization", "Bearer tecnico"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.textoIngresado.humedad").value("22.0"))
                .andExpect(jsonPath("$.textoIngresado.temperatura").value("023"))
                .andExpect(jsonPath("$.textoIngresado.lectura1Min").value("10.00"));
        api.perform(put("/api/protocolos/" + id).header("Authorization", "Bearer tecnico")
                        .contentType("application/json").content(correccion.replace("\"22.0\"", "\"23.0\"")))
                .andExpect(status().isBadRequest());
        api.perform(patch("/api/protocolos/" + id + "/formalizar").header("Authorization", "Bearer tecnico"))
                .andExpect(status().isForbidden());
        api.perform(patch("/api/protocolos/" + id + "/formalizar").header("Authorization", "Bearer ingeniero"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("FORMALIZADO"));

        api.perform(put("/api/protocolos/" + id).header("Authorization", "Bearer tecnico")
                        .contentType("application/json").content(correccion))
                .andExpect(status().isConflict());
    }

    @Test
    void editaSoloBorradoresPropiosYConservaElHistorialAlAnular() throws Exception {
        var api = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        when(usuarios.perfil("Bearer autor")).thenReturn(new UsuariosClient.Usuario(21, 1, true));
        when(usuarios.perfil("Bearer otro")).thenReturn(new UsuariosClient.Usuario(22, 1, true));
        when(equipos.consultar("Bearer autor", 19L)).thenReturn(new ReferenciasClient.Equipo(19, 42, "TR-EDITAR", "Transformador", "Planta Demo"));
        when(usuarios.titular("Bearer autor", 42L))
                .thenReturn(new UsuariosClient.Titular(42L, 4L, "Empresa de prueba", "1-9"));
        when(instrumentos.consultar("Bearer autor", 7L))
                .thenReturn(new ReferenciasClient.Instrumento(7, "VIGENTE"));

        var borrador = protocolos.saveAndFlush(new Protocolo(19, 21, LocalDateTime.now(),
                BigDecimal.TEN, BigDecimal.TEN, Resultado.PENDIENTE));
        String ruta = "/api/protocolos/" + borrador.getId();
        String cambios = """
                {"equipoId":19,"instrumentoId":7,"temperatura":22,"humedad":50,
                 "tipoPrueba":"AISLACION","valores":{"lectura1Min":10,"lectura10Min":20}}
                """;
        api.perform(put(ruta).header("Authorization", "Bearer otro").contentType("application/json").content(cambios))
                .andExpect(status().isNotFound());
        api.perform(put(ruta).header("Authorization", "Bearer autor").contentType("application/json").content(cambios))
                .andExpect(status().isOk()).andExpect(jsonPath("$.resultadoGlobal").value("APROBADO"));
        api.perform(get("/api/protocolos/resumen-visita").header("Authorization", "Bearer autor"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.borradores").value(1))
                .andExpect(jsonPath("$.aprobadas").value(1));
        api.perform(patch(ruta + "/anular").header("Authorization", "Bearer autor"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("ANULADO"));
        api.perform(delete(ruta).header("Authorization", "Bearer autor")).andExpect(status().isConflict());

        var descartable = protocolos.saveAndFlush(new Protocolo(19, 21, LocalDateTime.now(),
                BigDecimal.TEN, BigDecimal.TEN, Resultado.PENDIENTE));
        api.perform(delete("/api/protocolos/" + descartable.getId()).header("Authorization", "Bearer autor"))
                .andExpect(status().isNoContent());
        assertFalse(protocolos.existsById(descartable.getId()));
    }

    @Test
    void exigeComentarioSoloSiLaVariacionSuperaVeinticincoPorCiento() throws Exception {
        var api = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        var autorizacion = "Bearer ingeniero";
        when(usuarios.perfil(autorizacion)).thenReturn(new UsuariosClient.Usuario(12L, 2L, true));

        when(equipos.consultar(autorizacion, 817L))
                .thenReturn(new ReferenciasClient.Equipo(817L, 42L, "TR", "Transformador", "Planta"));
        when(usuarios.titular(autorizacion, 42L))
                .thenReturn(new UsuariosClient.Titular(42L, 4L, "Empresa de prueba", "1-9"));
        guardarAislamientoRegistrado(817L, "100.00");
        var limite = "/api/protocolos/" + guardarAislamientoRegistrado(817L, "125.00");
        api.perform(get(limite + "/variacion").header("Authorization", autorizacion))
                .andExpect(status().isOk()).andExpect(jsonPath("$.significativa").value(false));
        api.perform(patch(limite + "/formalizar").header("Authorization", autorizacion))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("FORMALIZADO"));

        var superior = "/api/protocolos/" + guardarAislamientoRegistrado(817L, "156.26");
        api.perform(get(superior + "/variacion").header("Authorization", autorizacion))
                .andExpect(status().isOk()).andExpect(jsonPath("$.significativa").value(true));
        api.perform(patch(superior + "/formalizar").header("Authorization", autorizacion))
                .andExpect(status().isConflict());
        api.perform(patch(superior + "/formalizar").header("Authorization", autorizacion)
                        .contentType("application/json").content("""
                                {"texto":"  Cambio de condiciones de prueba  "}
                                """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("FORMALIZADO"))
                .andExpect(jsonPath("$.comentarioVariacion").value("Cambio de condiciones de prueba"));
    }

    @Test
    void devuelveUnaSolaActaAlRecibirSolicitudesConcurrentes() throws Exception {
        var api = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        long equipoId = 818L;
        var ruta = "/api/protocolos/" + guardarAislamientoRegistrado(equipoId, "100.00") + "/acta";
        var autorizacion = "Bearer tecnico-acta";
        var solicitudEnCurso = new CountDownLatch(1);
        var segundaAutenticada = new CountDownLatch(2);
        var liberar = new CountDownLatch(1);
        when(usuarios.perfil(autorizacion)).thenAnswer(invocacion -> {
            segundaAutenticada.countDown();
            return new UsuariosClient.Usuario(11L, 1L, true);
        });
        when(equipos.consultar(autorizacion, equipoId))
                .thenReturn(new ReferenciasClient.Equipo(equipoId, 42L, "TR-ACTA", "Transformador", "Planta QA"));
        when(usuarios.titular(autorizacion, 42L)).thenAnswer(invocacion -> {
            solicitudEnCurso.countDown();
            assertTrue(liberar.await(10, TimeUnit.SECONDS));
            return new UsuariosClient.Titular(42L, 4L, "Empresa de prueba", "1-9");
        });

        try (var ejecutor = Executors.newFixedThreadPool(2)) {
            try {
                var primera = ejecutor.submit(() ->
                        api.perform(post(ruta).header("Authorization", autorizacion))
                                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
                assertTrue(solicitudEnCurso.await(10, TimeUnit.SECONDS));
                var segunda = ejecutor.submit(() ->
                        api.perform(post(ruta).header("Authorization", autorizacion))
                                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
                assertTrue(segundaAutenticada.await(10, TimeUnit.SECONDS));
                liberar.countDown();
                assertEquals(primera.get(10, TimeUnit.SECONDS), segunda.get(10, TimeUnit.SECONDS));
                verify(usuarios, times(1)).titular(autorizacion, 42L);
            } finally {
                liberar.countDown();
            }
        }
    }

    private Long guardarAislamientoRegistrado(long equipoId, String lectura) {
        var valor = new BigDecimal(lectura);
        var protocolo = new Protocolo(equipoId, 11, LocalDateTime.of(2026, 11, 25, 9, 30),
                BigDecimal.valueOf(20), BigDecimal.valueOf(50), Resultado.APROBADO);
        var valores = new Medicion.Valores(valor, valor.multiply(BigDecimal.TWO),
                null, null, null, null, null, null, null);
        new Medicion(protocolo, Tipo.AISLACION, 3, valores, BigDecimal.TWO);
        protocolo.cerrarBorrador();
        return protocolos.saveAndFlush(protocolo).getId();
    }

    @Test
    void guardaBorradorYMedicionEnLaMismaOperacion() {
        var protocolo = new Protocolo(1, 2, LocalDateTime.of(2026, 11, 25, 9, 30),
                BigDecimal.valueOf(20), BigDecimal.valueOf(50), Resultado.APROBADO);
        var valores = new Medicion.Valores(BigDecimal.TEN, BigDecimal.valueOf(20),
                null, null, null, null, null, null, null);
        var medicion = new Medicion(protocolo, Tipo.AISLACION, 3, valores, BigDecimal.TWO);

        protocolos.saveAndFlush(protocolo);

        assertEquals(Protocolo.Estado.BORRADOR, protocolos.findById(protocolo.getId()).orElseThrow().getEstado());
        assertTrue(mediciones.existsByInstrumentoId(3));
        assertNotNull(medicion.getId());
    }
}
