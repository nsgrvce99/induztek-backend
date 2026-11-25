package cl.induztek.ms_equipos.service;

import cl.induztek.ms_equipos.dto.EquipoResponse;
import cl.induztek.ms_equipos.dto.NuevoEquipo;
import cl.induztek.ms_equipos.model.Equipo;
import cl.induztek.ms_equipos.model.Instalacion;
import cl.induztek.ms_equipos.repository.EquipoRepository;
import cl.induztek.ms_equipos.repository.InstalacionRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@Validated
@Transactional(readOnly = true)
public class EquipoService {
    private final EquipoRepository equipos;
    private final InstalacionRepository instalaciones;

    public EquipoService(EquipoRepository equipos, InstalacionRepository instalaciones) {
        this.equipos = equipos;
        this.instalaciones = instalaciones;
    }

    @Transactional
    public EquipoResponse registrar(@Valid NuevoEquipo solicitud) {
        Instalacion instalacion = instalaciones.findById(solicitud.instalacionId()).filter(p -> !p.isArchivada())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Instalacion no encontrada"));
        Equipo equipo = new Equipo(solicitud.tag(), solicitud.tipoEquipo().trim(), solicitud.marca().trim(),
                solicitud.serie().trim(), instalacion);
        return EquipoResponse.desde(equipos.saveAndFlush(equipo));
    }

    @Transactional
    public void archivar(long id) {
        buscar(id).archivar();
        equipos.flush();
    }

    public java.util.List<Long> idsDelCliente(long id) {
        return equipos.idsDelCliente(id);
    }

    public EquipoResponse consultar(long id) {
        return EquipoResponse.desde(buscar(id));
    }

    public EquipoResponse consultarDelCliente(long id, long idCliente) {
        Equipo equipo = buscar(id);
        if (equipo.isArchivado() || !Objects.equals(equipo.getIdCliente(), idCliente)) {
            throw equipoNoEncontrado();
        }
        return EquipoResponse.desde(equipo);
    }

    public Page<EquipoResponse> listarPorInstalacion(long idInstalacion, Pageable pagina) {
        if (!instalaciones.existsById(idInstalacion)) {
            throw new ResponseStatusException(NOT_FOUND, "Instalacion no encontrada");
        }
        return equipos.findByInstalacionId(idInstalacion, pagina).map(EquipoResponse::desde);
    }

    public Page<EquipoResponse> buscarDelCliente(long idCliente, String busqueda, Pageable pagina) {
        return equipos.buscarDelCliente(idCliente, busqueda.trim(), pagina).map(EquipoResponse::desde);
    }

    private Equipo buscar(long id) {
        return equipos.findById(id).orElseThrow(EquipoService::equipoNoEncontrado);
    }

    private static ResponseStatusException equipoNoEncontrado() {
        return new ResponseStatusException(NOT_FOUND, "Equipo no encontrado");
    }
}
