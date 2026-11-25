package cl.induztek.ms_instrumentos.service;

import cl.induztek.comun.CoordinacionReferencias;
import cl.induztek.comun.DependenciasProtocolos;
import cl.induztek.ms_instrumentos.dto.InstrumentoResponse;
import cl.induztek.ms_instrumentos.dto.InstrumentoSolicitud;
import cl.induztek.ms_instrumentos.model.Instrumento;
import cl.induztek.ms_instrumentos.repository.InstrumentoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

@Service
public class InstrumentoService {
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd-MM-uuuu")
            .withResolverStyle(ResolverStyle.STRICT);
    private static final ZoneId CHILE = ZoneId.of("America/Santiago");
    private final InstrumentoRepository instrumentos;
    private final CoordinacionReferencias coordinacion;
    private final DependenciasProtocolos dependencias;

    public InstrumentoService(InstrumentoRepository instrumentos, CoordinacionReferencias coordinacion,
                               DependenciasProtocolos dependencias) {
        this.instrumentos = instrumentos;
        this.coordinacion = coordinacion;
        this.dependencias = dependencias;
    }

    public Page<InstrumentoResponse> listar(String q, Pageable pagina) {
        String filtro = q.trim();
        LocalDate hoy = LocalDate.now(CHILE);
        return instrumentos.findByNombreContainingIgnoreCaseOrMarcaContainingIgnoreCase(filtro, filtro, pagina)
                .map(instrumento -> respuesta(instrumento, hoy));
    }

    public InstrumentoResponse consultar(long id) {
        return respuesta(buscar(id), LocalDate.now(CHILE));
    }

    public InstrumentoResponse registrar(InstrumentoSolicitud datos) {
        var instrumento = new Instrumento(datos.nombre(), datos.marca(), datos.serie(), fecha(datos.fechaVencimiento()));
        return respuesta(instrumentos.save(instrumento), LocalDate.now(CHILE));
    }

    @Transactional
    public InstrumentoResponse actualizar(long id, InstrumentoSolicitud datos) {
        Instrumento instrumento = buscar(id);
        instrumento.actualizar(datos.nombre(), datos.marca(), datos.serie(), fecha(datos.fechaVencimiento()));
        return respuesta(instrumento, LocalDate.now(CHILE));
    }

    public void eliminar(long id, String autorizacion) {
        coordinacion.ejecutar(() -> {
            Instrumento instrumento = buscar(id);
            DependenciasProtocolos.verificar(() -> dependencias.instrumentoEnUso(autorizacion, id),
                    "El instrumento se usa en un protocolo");
            instrumentos.delete(instrumento);
            instrumentos.flush();
            return null;
        });
    }

    private Instrumento buscar(long id) {
        return instrumentos.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Instrumento no encontrado"));
    }

    private static InstrumentoResponse respuesta(Instrumento instrumento, LocalDate hoy) {
        LocalDate vencimiento = instrumento.getFechaVencimiento();
        return new InstrumentoResponse(instrumento.getId(), instrumento.getNombre(), instrumento.getMarca(),
                instrumento.getSerie(), vencimiento.format(FORMATO), Calibracion.evaluar(vencimiento, hoy));
    }

    private static LocalDate fecha(String texto) {
        try {
            return LocalDate.parse(texto, FORMATO);
        } catch (DateTimeParseException error) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fecha invalida. Usa dd-MM-yyyy");
        }
    }
}