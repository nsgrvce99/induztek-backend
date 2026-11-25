package cl.induztek.ms_equipos.service;

import cl.induztek.ms_equipos.client.UsuariosClient;
import cl.induztek.ms_equipos.model.Instalacion;
import cl.induztek.ms_equipos.repository.EquipoRepository;
import cl.induztek.ms_equipos.repository.InstalacionRepository;
import feign.FeignException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.*;

@Service
@Transactional(readOnly = true)
public class InstalacionService {
    private final InstalacionRepository instalaciones;
    private final EquipoRepository equipos;
    private final UsuariosClient usuarios;

    public InstalacionService(InstalacionRepository instalaciones, EquipoRepository equipos,
                              UsuariosClient usuarios) {
        this.instalaciones = instalaciones;
        this.equipos = equipos;
        this.usuarios = usuarios;
    }

    @Transactional
    public Instalacion registrar(String nombre, long idCliente, String autorizacion) {
        UsuariosClient.Usuario titular;
        try {
            titular = usuarios.buscar(autorizacion, idCliente);
        } catch (FeignException error) {
            throw switch (error.status()) {
                case 401 -> new ResponseStatusException(UNAUTHORIZED, "Sesion invalida");
                case 403 -> new ResponseStatusException(FORBIDDEN, "Operacion no autorizada");
                case 404 -> new ResponseStatusException(NOT_FOUND, "Cliente no encontrado");
                default -> new ResponseStatusException(SERVICE_UNAVAILABLE, "Servicio de Usuarios no disponible");
            };
        }
        if (!titular.esCliente() || titular.archivado()) {
            throw new ResponseStatusException(BAD_REQUEST, "El propietario debe tener rol CLIENTE");
        }
        return instalaciones.saveAndFlush(new Instalacion(nombre.trim(), titular.id()));
    }

    public Instalacion consultar(long id) {
        return instalaciones.findById(id).filter(instalacion -> !instalacion.isArchivada())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Instalacion no encontrada"));
    }

    public Page<Instalacion> buscar(String nombre, Pageable pagina) {
        return instalaciones.findByArchivadaFalseAndNombreContainingIgnoreCase(nombre.trim(), pagina);
    }

    @Transactional
    public void eliminar(long id) {
        Instalacion instalacion = consultar(id);
        if (equipos.existsByInstalacionIdAndArchivadoFalse(id)) {
            throw new ResponseStatusException(CONFLICT, "La instalacion tiene equipos asociados");
        }
        instalacion.archivar();
        instalaciones.flush();
    }
}
