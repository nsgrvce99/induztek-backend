package cl.induztek.ms_equipos;

import cl.induztek.ms_equipos.dto.EquipoResponse;
import cl.induztek.ms_equipos.dto.NuevoEquipo;
import cl.induztek.ms_equipos.model.Equipo;
import cl.induztek.ms_equipos.model.Instalacion;
import cl.induztek.ms_equipos.repository.EquipoRepository;
import cl.induztek.ms_equipos.repository.InstalacionRepository;
import cl.induztek.ms_equipos.service.EquipoService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EquipoServiceTest {
    private final EquipoRepository equipos = mock(EquipoRepository.class);
    private final InstalacionRepository instalaciones = mock(InstalacionRepository.class);
    private final EquipoService servicio = new EquipoService(equipos, instalaciones);
    private final NuevoEquipo solicitud = new NuevoEquipo("TR-01", "Transformador", "Rhona", "RH-1", 1);

    @Test
    void registrarUsaElClientePropietarioDeLaInstalacion() {
        when(instalaciones.findById(1L)).thenReturn(Optional.of(new Instalacion("Planta", 42L)));
        when(equipos.saveAndFlush(any(Equipo.class))).thenAnswer(llamada -> llamada.getArgument(0));

        EquipoResponse respuesta = servicio.registrar(solicitud);

        assertEquals(42L, respuesta.idCliente());
        assertEquals("Planta", respuesta.instalacion());
        assertEquals("Primario", respuesta.clasificacion());
        assertEquals("", respuesta.potenciaTension());
    }

    @Test
    void noRegistraEquiposSinInstalacionExistente() {
        when(instalaciones.findById(1L)).thenReturn(Optional.empty());

        var error = assertThrows(ResponseStatusException.class, () -> servicio.registrar(solicitud));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verifyNoInteractions(equipos);
    }

    @Test
    void soloElPropietarioPuedeConsultarEnElFlujoDeCliente() {
        var equipo = new Equipo("TR-01", "Transformador", "Rhona", "RH-1", new Instalacion("Planta", 42L));
        when(equipos.findById(9L)).thenReturn(Optional.of(equipo));

        assertEquals(42L, servicio.consultarDelCliente(9, 42).idCliente());
        var error = assertThrows(ResponseStatusException.class, () -> servicio.consultarDelCliente(9, 43));
        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
    }
}
