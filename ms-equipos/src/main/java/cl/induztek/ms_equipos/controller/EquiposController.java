package cl.induztek.ms_equipos.controller;

import cl.induztek.ms_equipos.client.UsuariosClient.Usuario;
import cl.induztek.ms_equipos.dto.EquipoResponse;
import cl.induztek.comun.CoordinacionReferencias;
import cl.induztek.comun.DependenciasProtocolos;
import cl.induztek.ms_equipos.dto.NuevoEquipo;
import cl.induztek.ms_equipos.model.Instalacion;
import cl.induztek.ms_equipos.service.EquipoService;
import cl.induztek.ms_equipos.service.InstalacionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class EquiposController {
    private final EquipoService equipos;
    private final InstalacionService instalaciones;

    private final CoordinacionReferencias coordinacion;
    private final DependenciasProtocolos dependencias;

    public EquiposController(EquipoService equipos, InstalacionService instalaciones,
                             CoordinacionReferencias coordinacion, DependenciasProtocolos dependencias) {
        this.equipos = equipos;
        this.instalaciones = instalaciones;
        this.coordinacion = coordinacion;
        this.dependencias = dependencias;
    }

    @GetMapping("/equipos/{id}")
    EquipoResponse equipo(@PathVariable long id, @AuthenticationPrincipal Usuario perfil) {
        var equipo = referenciaEquipo(id, perfil);
        if (equipo.archivado()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Equipo no encontrado");
        return equipo;
    }

    @GetMapping("/equipos/{id}/referencia")
    EquipoResponse referenciaEquipo(@PathVariable long id, @AuthenticationPrincipal Usuario perfil) {
        return perfil.esCliente() ? equipos.consultarDelCliente(id, perfil.id()) : equipos.consultar(id);
    }

    @GetMapping("/equipos/mis-equipos")
    PagedModel<EquipoResponse> propios(@AuthenticationPrincipal Usuario perfil,
            @RequestParam(defaultValue = "") String q, @PageableDefault(sort = "id") Pageable pagina) {
        return new PagedModel<>(equipos.buscarDelCliente(perfil.id(), q, pagina));
    }

    @PostMapping("/equipos")
    @ResponseStatus(HttpStatus.CREATED)
    EquipoResponse registrarEquipo(@Valid @RequestBody NuevoEquipo equipo) {
        return coordinacion.ejecutar(() -> equipos.registrar(equipo));
    }

    @DeleteMapping("/equipos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void eliminarEquipo(@PathVariable long id, @RequestHeader(HttpHeaders.AUTHORIZATION) String autorizacion) {
        coordinacion.ejecutar(() -> {
            equipos.consultar(id);
            DependenciasProtocolos.verificar(() -> dependencias.equipoFormalizado(autorizacion, id),
                    "El equipo tiene protocolos formalizados");
            equipos.archivar(id);
            return null;
        });
    }

    @GetMapping("/equipos/dependencias/clientes/{id}")
    java.util.List<Long> equiposDelCliente(@PathVariable long id) {
        return equipos.idsDelCliente(id);
    }

    @GetMapping("/instalaciones")
    PagedModel<InstalacionResponse> instalaciones(@RequestParam(defaultValue = "") String q,
                                                 @PageableDefault(sort = "id") Pageable pagina) {
        Page<InstalacionResponse> resultado = instalaciones.buscar(q, pagina).map(InstalacionResponse::desde);
        return new PagedModel<>(resultado);
    }

    @GetMapping("/instalaciones/{id}")
    InstalacionResponse instalacion(@PathVariable long id) {
        return InstalacionResponse.desde(instalaciones.consultar(id));
    }

    @GetMapping("/instalaciones/{id}/equipos")
    PagedModel<EquipoResponse> equipos(@PathVariable long id, @PageableDefault(sort = "id") Pageable pagina) {
        return new PagedModel<>(equipos.listarPorInstalacion(id, pagina));
    }

    @PostMapping("/instalaciones")
    @ResponseStatus(HttpStatus.CREATED)
    InstalacionResponse registrarInstalacion(@Valid @RequestBody NuevaInstalacion solicitud,
                                             @RequestHeader(HttpHeaders.AUTHORIZATION) String autorizacion) {
        return coordinacion.ejecutar(() ->
                InstalacionResponse.desde(instalaciones.registrar(solicitud.nombre(), solicitud.idCliente(), autorizacion)));
    }

    @DeleteMapping("/instalaciones/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void eliminarInstalacion(@PathVariable long id) {
        coordinacion.ejecutar(() -> {
            instalaciones.eliminar(id);
            return null;
        });
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail conflicto() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "El registro ya existe o tiene dependencias");
    }

    record NuevaInstalacion(@NotBlank @Size(max = 120) String nombre, @Positive long idCliente) {}

    record InstalacionResponse(Long id, String nombre, Long idCliente) {
        static InstalacionResponse desde(Instalacion instalacion) {
            return new InstalacionResponse(instalacion.getId(), instalacion.getNombre(), instalacion.getIdCliente());
        }
    }
}
