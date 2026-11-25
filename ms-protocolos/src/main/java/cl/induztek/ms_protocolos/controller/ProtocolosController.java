package cl.induztek.ms_protocolos.controller;

import cl.induztek.ms_protocolos.client.ReferenciasClient;
import cl.induztek.comun.CoordinacionReferencias;
import cl.induztek.ms_protocolos.dto.FormatoMediciones;
import cl.induztek.ms_protocolos.client.UsuariosClient;
import cl.induztek.ms_protocolos.client.UsuariosClient.Usuario;
import cl.induztek.ms_protocolos.model.RecepcionActa;
import cl.induztek.ms_protocolos.repository.RecepcionActaRepository;
import cl.induztek.ms_protocolos.model.Medicion;
import cl.induztek.ms_protocolos.model.Protocolo;
import cl.induztek.ms_protocolos.model.Protocolo.Estado;
import cl.induztek.ms_protocolos.repository.ProtocoloRepository;
import cl.induztek.ms_protocolos.repository.MedicionRepository;
import cl.induztek.ms_protocolos.service.EvaluacionPrueba;
import cl.induztek.ms_protocolos.service.EvaluacionPrueba.Tipo;
import feign.FeignException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RestController
@RequestMapping("/api/protocolos")
public class ProtocolosController {
    private static final List<Estado> PRIVADOS = List.of(Estado.BORRADOR, Estado.ANULADO);
    private final ProtocoloRepository protocolos;
    private final MedicionRepository mediciones;
    private final RecepcionActaRepository actas;
    private final UsuariosClient usuarios;
    private final ReferenciasClient.Equipos equipos;
    private final ReferenciasClient.Instrumentos instrumentos;
    private final CoordinacionReferencias coordinacion;

    public ProtocolosController(ProtocoloRepository protocolos, MedicionRepository mediciones, ReferenciasClient.Equipos equipos,
                                ReferenciasClient.Instrumentos instrumentos, RecepcionActaRepository actas, UsuariosClient usuarios,
                                CoordinacionReferencias coordinacion) {
        this.coordinacion = coordinacion;
        this.protocolos = protocolos;
        this.mediciones = mediciones;
        this.actas = actas;
        this.usuarios = usuarios;
        this.equipos = equipos;
        this.instrumentos = instrumentos;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    Resumen registrar(@Valid @RequestBody NuevoProtocolo datos, @AuthenticationPrincipal Usuario perfil,
                       @RequestHeader(HttpHeaders.AUTHORIZATION) String autorizacion) {
        return coordinacion.ejecutar(() -> {
            equipoOperativo(datos.equipoId(), autorizacion);
            validarInstrumento(datos.instrumentoId(), autorizacion);
            var evaluacion = evaluar(datos.tipoPrueba(), datos.valores());
            var borrador = new Protocolo(datos.equipoId(), perfil.id(),
                    LocalDateTime.now(ZoneId.of("America/Santiago")), datos.temperatura(), datos.humedad(),
                    evaluacion.resultado());
            new Medicion(borrador, datos.tipoPrueba(), datos.instrumentoId(), datos.valores(), evaluacion.indicePolarizacion());
            borrador.conservarTexto(datos.formato());
            return Resumen.desde(protocolos.saveAndFlush(borrador));
        });
    }

    @PutMapping("/{id}")
    Resumen editar(@PathVariable long id, @Valid @RequestBody NuevoProtocolo datos,
                    @AuthenticationPrincipal Usuario perfil,
                    @RequestHeader(HttpHeaders.AUTHORIZATION) String autorizacion) {
        return coordinacion.ejecutar(() -> {
            var borrador = propio(id, perfil);
            if (borrador.getEstado() == Estado.FORMALIZADO || borrador.getEstado() == Estado.ANULADO
                    || borrador.getEquipoId() != datos.equipoId()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "El protocolo no se puede editar");
            }
            equipoOperativo(datos.equipoId(), autorizacion);
            validarInstrumento(datos.instrumentoId(), autorizacion);
            var evaluacion = evaluar(datos.tipoPrueba(), datos.valores());
            borrador.editar(datos.temperatura(), datos.humedad(), evaluacion.resultado());
            new Medicion(borrador, datos.tipoPrueba(), datos.instrumentoId(), datos.valores(), evaluacion.indicePolarizacion());
            borrador.conservarTexto(datos.formato());
            return Resumen.desde(protocolos.saveAndFlush(borrador));
        });
    }

    @PatchMapping("/{id}/anular")
    @Transactional
    Resumen anular(@PathVariable long id, @AuthenticationPrincipal Usuario perfil) {
        var protocolo = propio(id, perfil);
        if (protocolo.getEstado() == Estado.FORMALIZADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El protocolo formalizado es un documento oficial");
        }
        protocolo.anular();
        return Resumen.desde(protocolos.saveAndFlush(protocolo));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    void eliminar(@PathVariable long id, @AuthenticationPrincipal Usuario perfil) {
        var protocolo = propio(id, perfil);
        if (protocolo.getEstado() != Estado.BORRADOR) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo se eliminan borradores. Usa anular para conservar el historial");
        }
        protocolos.delete(protocolo);
        protocolos.flush();
    }

    @GetMapping("/resumen-visita")
    ResumenVisita resumenVisita(@AuthenticationPrincipal Usuario perfil) {
        var conteos = protocolos.resumir(perfil.id());
        long total = 0, aprobadas = 0, precaucion = 0, rechazadas = 0;
        long borradores = 0, pendientes = 0, formalizadas = 0;
        for (var conteo : conteos) {
            long cantidad = conteo.getCantidad();
            total += cantidad;
            switch (conteo.getEstado()) {
                case BORRADOR -> borradores += cantidad;
                case REGISTRADO_TERRENO, OBSERVADO, PRECAUCION -> pendientes += cantidad;
                case FORMALIZADO -> formalizadas += cantidad;
                case ANULADO -> { }
            }
            switch (conteo.getResultado()) {
                case APROBADO -> aprobadas += cantidad;
                case PRECAUCION -> precaucion += cantidad;
                case RECHAZADO -> rechazadas += cantidad;
                case PENDIENTE, REVISION_MANUAL -> { }
            }
        }
        return new ResumenVisita(total, aprobadas, precaucion, rechazadas, borradores, pendientes, formalizadas);
    }

    private Protocolo propio(long id, Usuario perfil) {
        return protocolos.findById(id).filter(p -> p.getTecnicoId() == perfil.id())
                .orElseThrow(ProtocolosController::noEncontrado);
    }

    private static EvaluacionPrueba.Evaluacion evaluar(Tipo tipo, Medicion.Valores valores) {
        boolean rele = tipo == Tipo.RELE;
        return EvaluacionPrueba.evaluar(tipo,
                rele ? valores.tiempoEsperado() : valores.lectura1Min(),
                rele ? valores.tiempoMedido() : valores.lectura10Min());
    }

    record ResumenVisita(long total, long aprobadas, long precaucion, long rechazadas,
                        long borradores, long pendientes, long formalizadas) {}

    @GetMapping("/mis-protocolos")
    PagedModel<Resumen> propios(@AuthenticationPrincipal Usuario perfil,
                                 @PageableDefault(sort = "id", direction = org.springframework.data.domain.Sort.Direction.DESC)
                                 Pageable pagina) {
        return new PagedModel<>(protocolos.findByTecnicoId(perfil.id(), pagina).map(Resumen::desde));
    }

    @PatchMapping("/{id}/cerrar")
    Resumen cerrar(@PathVariable long id, @AuthenticationPrincipal Usuario perfil,
                   @RequestHeader(HttpHeaders.AUTHORIZATION) String autorizacion) {
        return coordinacion.ejecutar(() -> {
            Protocolo protocolo = protocolos.findById(id).orElseThrow(ProtocolosController::noEncontrado);
            if (protocolo.getTecnicoId() != perfil.id()) {
                throw noEncontrado();
            }
            if (protocolo.getEstado() != Estado.BORRADOR) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo se puede cerrar un borrador");
            }
            equipoOperativo(protocolo.getEquipoId(), autorizacion);
            protocolo.cerrarBorrador();
            return Resumen.desde(protocolos.saveAndFlush(protocolo));
        });
    }

    @ExceptionHandler(org.springframework.dao.OptimisticLockingFailureException.class)
    org.springframework.http.ProblemDetail cambioConcurrente() {
        return org.springframework.http.ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "El protocolo cambio. Consultalo nuevamente");
    }

    @PostMapping("/{id}/acta")
    @ResponseStatus(HttpStatus.CREATED)
    ActaResponse entregarActa(@PathVariable long id, @AuthenticationPrincipal Usuario perfil,
                               @RequestHeader(HttpHeaders.AUTHORIZATION) String autorizacion) {
        return coordinacion.ejecutar(() -> {
            Protocolo protocolo = protocolos.findByIdAndTecnicoId(id, perfil.id())
                    .orElseThrow(ProtocolosController::noEncontrado);
            var existente = actas.findByProtocoloId(id);
            if (existente.isPresent()) {
                return ActaResponse.desde(existente.get());
            }
            if (protocolo.getEstado() != Estado.REGISTRADO_TERRENO || protocolo.getMediciones().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "El acta requiere un protocolo registrado y medido");
            }
            var equipo = equipo(protocolo.getEquipoId(), perfil, autorizacion);
            UsuariosClient.Titular titular;
            try {
                titular = usuarios.titular(autorizacion, equipo.idCliente());
            } catch (FeignException error) {
                throw errorRemoto(error, "Titular");
            }
            if (equipo.archivado() || titular == null || titular.archivado() || titular.rolId() != 4 || titular.nombre() == null || titular.rut() == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "El equipo no tiene un cliente receptor valido");
            }
            var acta = new RecepcionActa(protocolo, titular.nombre(), titular.rut(),
                    "Representante Legal / Jefe de Planta",
                    LocalDateTime.now(ZoneId.of("America/Santiago")).truncatedTo(ChronoUnit.MICROS));
            return ActaResponse.desde(actas.saveAndFlush(acta));
        });
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    org.springframework.http.ProblemDetail conflictoDeDatos() {
        return org.springframework.http.ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "La operacion entra en conflicto con los datos registrados");
    }

    record ActaResponse(Long id, long protocoloId, String nombreReceptor, String rutReceptor, String cargoReceptor,
                        LocalDateTime fechaHoraRecepcion) {
        static ActaResponse desde(RecepcionActa acta) {
            return new ActaResponse(acta.getId(), acta.getProtocolo().getId(), acta.getNombreReceptor(),
                    acta.getRutReceptor(), acta.getCargoReceptor(), acta.getFechaHoraRecepcion());
        }
    }

    @GetMapping("/pendientes")
    PagedModel<Resumen> pendientes(@PageableDefault(sort = "id", direction = org.springframework.data.domain.Sort.Direction.DESC)
                                     Pageable pagina) {
        return new PagedModel<>(protocolos.findByEstado(Estado.REGISTRADO_TERRENO, pagina).map(Resumen::desde));
    }

    @GetMapping("/{id}/variacion")
    @Transactional(readOnly = true)
    Variacion variacion(@PathVariable long id) {
        return new Variacion(hayVariacion(buscarRegistrado(id)));
    }

    record Variacion(boolean significativa) {}

    @PatchMapping("/{id}/formalizar")
    Resumen formalizar(@PathVariable long id, @Valid @RequestBody(required = false) Comentario datos,
                       @RequestHeader(HttpHeaders.AUTHORIZATION) String autorizacion) {
        return coordinacion.ejecutar(() -> {
            Protocolo protocolo = buscarRegistrado(id);
            String comentario = datos == null || datos.texto() == null ? "" : datos.texto().trim();
            if (hayVariacion(protocolo) && comentario.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Se requiere comentario por variacion superior al 25%");
            }
            equipoOperativo(protocolo.getEquipoId(), autorizacion);
            protocolo.formalizar(comentario.isEmpty() ? null : comentario);
            return Resumen.desde(protocolos.saveAndFlush(protocolo));
        });
    }

    @PatchMapping("/{id}/observar")
    @Transactional
    Resumen observar(@PathVariable long id, @Valid @RequestBody Observacion datos) {
        Protocolo protocolo = buscarRegistrado(id);
        protocolo.observar(datos.motivo().trim());
        return Resumen.desde(protocolos.saveAndFlush(protocolo));
    }

    private Protocolo buscarRegistrado(long id) {
        var protocolo = protocolos.findById(id).orElseThrow(ProtocolosController::noEncontrado);
        if (protocolo.getEstado() != Estado.REGISTRADO_TERRENO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo se puede revisar un protocolo registrado en terreno");
        }
        return protocolo;
    }

    private boolean hayVariacion(Protocolo protocolo) {
        for (Medicion actual : protocolo.getMediciones()) {
            if (actual.getLectura1Min() == null) {
                continue;
            }
            var anterior = mediciones.findFirstByProtocoloEquipoIdAndTipoPruebaAndProtocoloIdLessThanOrderByProtocoloIdDescIdAsc(
                    protocolo.getEquipoId(), actual.getTipoPrueba(), protocolo.getId());
            var referencia = anterior.map(Medicion::getLectura1Min).orElse(null);
            if (EvaluacionPrueba.variacionSignificativa(referencia, actual.getLectura1Min())) {
                return true;
            }
        }
        return false;
    }

    record Comentario(@Size(max = 1000) String texto) {}
    record Observacion(@NotBlank @Size(max = 1000) String motivo) {}

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    Detalle detalle(@PathVariable long id, @AuthenticationPrincipal Usuario perfil,
                    @RequestHeader(HttpHeaders.AUTHORIZATION) String autorizacion) {
        Protocolo protocolo = protocolos.findById(id).orElseThrow(ProtocolosController::noEncontrado);
        if (perfil.esCliente() && PRIVADOS.contains(protocolo.getEstado())) {
            throw noEncontrado();
        }
        var equipo = equipo(protocolo.getEquipoId(), perfil, autorizacion);
        PersonaReferencia titular = null;
        PersonaReferencia tecnico = null;
        ActaResponse acta = null;
        if (perfil.esPersonal()) {
            titular = persona(autorizacion, equipo.idCliente());
            tecnico = persona(autorizacion, protocolo.getTecnicoId());
            acta = actas.findByProtocoloId(id).map(ActaResponse::desde).orElse(null);
        }
        return new Detalle(Resumen.desde(protocolo), protocolo.getMediciones().stream()
                .map(MedicionResponse::desde).toList(), equipo, titular, tecnico, acta, protocolo.getTextoMediciones());
    }

    @GetMapping("/equipo/{equipoId}")
    PagedModel<Resumen> historial(@PathVariable long equipoId, @AuthenticationPrincipal Usuario perfil,
                                  @RequestHeader(HttpHeaders.AUTHORIZATION) String autorizacion,
                                  @PageableDefault(sort = "id", direction = org.springframework.data.domain.Sort.Direction.DESC)
                                  Pageable pagina) {
        equipo(equipoId, perfil, autorizacion);
        Page<Protocolo> resultado = perfil.esCliente()
                ? protocolos.findByEquipoIdAndEstadoNotIn(equipoId, PRIVADOS, pagina)
                : protocolos.findByEquipoId(equipoId, pagina);
        return new PagedModel<>(resultado.map(Resumen::desde));
    }

    private ReferenciasClient.Equipo equipo(long id, Usuario perfil, String autorizacion) {
        try {
            var equipo = equipos.consultar(autorizacion, id);
            if (perfil.esCliente() && equipo.idCliente() != perfil.id()) {
                throw noEncontrado();
            }
            return equipo;
        } catch (FeignException error) {
            throw errorRemoto(error, "Equipo");
        }
    }

    @GetMapping("/dependencias/equipos/{id}")
    boolean equipoFormalizado(@PathVariable long id) {
        return protocolos.existsByEquipoIdAndEstado(id, Estado.FORMALIZADO);
    }

    @GetMapping("/dependencias/instrumentos/{id}")
    boolean instrumentoEnUso(@PathVariable long id) {
        return mediciones.existsByInstrumentoId(id);
    }

    @GetMapping("/dependencias/clientes/{id}")
    boolean clienteFormalizado(@PathVariable long id, @RequestHeader(HttpHeaders.AUTHORIZATION) String autorizacion) {
        try {
            var ids = equipos.idsDelCliente(autorizacion, id);
            return !ids.isEmpty() && protocolos.existsByEquipoIdInAndEstado(ids, Estado.FORMALIZADO);
        } catch (FeignException error) {
            throw errorRemoto(error, "Equipos");
        }
    }

    private void equipoOperativo(long id, String autorizacion) {
        try {
            var equipo = equipos.consultar(autorizacion, id);
            if (equipo == null) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Equipo no disponible");
            if (equipo.archivado()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "El equipo fue eliminado");
            }
            var titular = usuarios.titular(autorizacion, equipo.idCliente());
            if (titular == null) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Cliente no disponible");
            if (titular.archivado()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "El cliente fue eliminado");
            }
        } catch (FeignException error) {
            throw errorRemoto(error, "Equipo o cliente");
        }
    }

    private void validarInstrumento(long id, String autorizacion) {
        try {
            var instrumento = instrumentos.consultar(autorizacion, id);
            if (!List.of("VIGENTE", "POR_VENCER").contains(instrumento.calibracion())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "El instrumento no tiene calibracion vigente");
            }
        } catch (FeignException error) {
            throw errorRemoto(error, "Instrumento");
        }
    }

    private static ResponseStatusException errorRemoto(FeignException error, String recurso) {
        return switch (error.status()) {
            case 401 -> new ResponseStatusException(HttpStatus.UNAUTHORIZED);
            case 403 -> new ResponseStatusException(HttpStatus.FORBIDDEN);
            case 404 -> new ResponseStatusException(HttpStatus.NOT_FOUND, recurso + " no encontrado");
            default -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No se pudo consultar " + recurso);
        };
    }

    private static ResponseStatusException noEncontrado() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Protocolo no encontrado");
    }

    record NuevoProtocolo(@Positive long equipoId, @Positive long instrumentoId,
                          @NotNull @Digits(integer = 5, fraction = 3) BigDecimal temperatura,
                          @NotNull @DecimalMin("0") @DecimalMax("100")
                          @Digits(integer = 3, fraction = 3) BigDecimal humedad,
                          @NotNull Tipo tipoPrueba, @NotNull @Valid Medicion.Valores valores,
                          @Size(max = 11) java.util.Map<String, String> textoIngresado) {
        java.util.Map<String, String> formato() {
            return FormatoMediciones.conservar(temperatura, humedad, valores, textoIngresado);
        }
    }

    record Resumen(Long id, long equipoId, long tecnicoId, LocalDateTime fecha, BigDecimal temperatura,
                   BigDecimal humedad, String resultadoGlobal, Estado estado, String motivoObservacion,
                   String comentarioVariacion) {
        static Resumen desde(Protocolo p) {
            return new Resumen(p.getId(), p.getEquipoId(), p.getTecnicoId(), p.getFechaHora(), p.getTemperatura(),
                    p.getHumedad(), p.getResultadoGlobal().name(), p.getEstado(), p.getMotivoObservacion(),
                    p.getComentarioVariacion());
        }
    }

    private PersonaReferencia persona(String autorizacion, long id) {
        try {
            var persona = usuarios.titular(autorizacion, id);
            return new PersonaReferencia(persona.id(), persona.nombre(), persona.archivado());
        } catch (FeignException error) {
            throw errorRemoto(error, "Usuario");
        }
    }

    record PersonaReferencia(long id, String nombre, boolean archivado) {}

    record Detalle(Resumen protocolo, List<MedicionResponse> mediciones, ReferenciasClient.Equipo equipo,
                   PersonaReferencia cliente, PersonaReferencia tecnico, ActaResponse acta,
                   java.util.Map<String, String> textoIngresado) {}

    record MedicionResponse(Long id, Tipo tipoPrueba, long instrumentoId, BigDecimal indicePolarizacion,
                            Medicion.Valores valores) {
        static MedicionResponse desde(Medicion m) {
            return new MedicionResponse(m.getId(), m.getTipoPrueba(), m.getInstrumentoId(), m.getIndicePolarizacion(),
                    new Medicion.Valores(m.getLectura1Min(), m.getLectura10Min(), m.getCorrienteAjuste(),
                            m.getCorrienteOperacion(), m.getTiempoEsperado(), m.getTiempoMedido(),
                            m.getLecturaDevanadoA(), m.getLecturaDevanadoB(), m.getLecturaDevanadoC()));
        }
    }
}
