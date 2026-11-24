package cl.induztek.ms_usuarios.controller;

import cl.induztek.ms_usuarios.dto.*;
import cl.induztek.ms_usuarios.service.UsuarioService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(
        UsuarioService usuarioService
    ) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/{id}")
    public UsuarioResponse buscarPorId(
        @PathVariable Long id
    ) {

        return usuarioService.buscarPorId(id);
    }

    @GetMapping("/personal")
    public List<UsuarioResponse> listarPersonal(
        @RequestParam(defaultValue = "") String query,
        @RequestParam(defaultValue = "20") int limit,
        @RequestParam(defaultValue = "0") int offset
    ) {

        return usuarioService.listarPersonal(
            query, limit, offset
        );
    }

    @PostMapping("/personal")
    @ResponseStatus(HttpStatus.CREATED)
    public PersonalCreadoResponse registrarPersonal(
        @Valid @RequestBody PersonalRequest request
    ) {

        return usuarioService.registrarPersonal(request);
    }

    @PutMapping("/{id}")
    public UsuarioResponse actualizarPersonal(
        @PathVariable Long id,
        @Valid @RequestBody PersonalRequest request
    ) {

        return usuarioService.actualizarPersonal(
            id, request
        );
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarPersonal(
        @PathVariable Long id
    ) {

        usuarioService.eliminarPersonal(id);
    }

    @PatchMapping("/{id}/foto")
    public UsuarioResponse actualizarFoto(
        @PathVariable Long id,
        @Valid @RequestBody FotoRequest request
    ) {

        return usuarioService.actualizarFoto(
            id, request
        );
    }

    @PatchMapping("/{id}/contrasena")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cambiarContrasena(
        @PathVariable Long id,
        @Valid @RequestBody CambiarContrasenaRequest request
    ) {

        usuarioService.cambiarContrasena(
            id, request
        );
    }
}