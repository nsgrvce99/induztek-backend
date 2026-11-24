package cl.induztek.ms_usuarios.controller;

import cl.induztek.ms_usuarios.dto.*;
import cl.induztek.ms_usuarios.service.UsuarioService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final UsuarioService usuarioService;

    public ClienteController(
        UsuarioService usuarioService
    ) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public List<UsuarioResponse> listarClientes(
        @RequestParam(defaultValue = "") String query,
        @RequestParam(defaultValue = "20") int limit,
        @RequestParam(defaultValue = "0") int offset
    ) {

        return usuarioService.listarClientes(
            query, limit, offset
        );
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse registrarCliente(
        @Valid @RequestBody ClienteRequest request
    ) {

        return usuarioService.registrarCliente(request);
    }

    @PutMapping("/{id}")
    public UsuarioResponse actualizarCliente(
        @PathVariable Long id,
        @Valid @RequestBody ClienteRequest request
    ) {

        return usuarioService.actualizarCliente(
            id, request
        );
    }

    @PatchMapping("/{id}/activar")
    public UsuarioResponse activarCliente(
        @PathVariable Long id,
        @Valid @RequestBody ActivacionRequest request
    ) {

        return usuarioService.activarCliente(
            id, request
        );
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarCliente(
        @PathVariable Long id
    ) {

        usuarioService.eliminarCliente(id);
    }
}