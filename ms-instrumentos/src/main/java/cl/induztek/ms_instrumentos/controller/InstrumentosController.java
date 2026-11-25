package cl.induztek.ms_instrumentos.controller;

import cl.induztek.ms_instrumentos.dto.InstrumentoResponse;
import cl.induztek.ms_instrumentos.dto.InstrumentoSolicitud;
import cl.induztek.ms_instrumentos.service.InstrumentoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/instrumentos")
public class InstrumentosController {
    private final InstrumentoService instrumentos;

    public InstrumentosController(InstrumentoService instrumentos) {
        this.instrumentos = instrumentos;
    }

    @GetMapping
    PagedModel<InstrumentoResponse> listar(@RequestParam(defaultValue = "") String q,
                                          @PageableDefault(sort = "id") Pageable pagina) {
        return new PagedModel<>(instrumentos.listar(q, pagina));
    }

    @GetMapping("/{id}")
    InstrumentoResponse consultar(@PathVariable long id) {
        return instrumentos.consultar(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    InstrumentoResponse registrar(@Valid @RequestBody InstrumentoSolicitud datos) {
        return instrumentos.registrar(datos);
    }

    @PutMapping("/{id}")
    InstrumentoResponse actualizar(@PathVariable long id, @Valid @RequestBody InstrumentoSolicitud datos) {
        return instrumentos.actualizar(id, datos);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void eliminar(@PathVariable long id, @RequestHeader(HttpHeaders.AUTHORIZATION) String autorizacion) {
        instrumentos.eliminar(id, autorizacion);
    }
}