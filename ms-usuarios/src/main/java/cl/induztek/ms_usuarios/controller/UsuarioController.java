package cl.induztek.ms_usuarios.controller;

import cl.induztek.ms_usuarios.dto.*;
import cl.induztek.ms_usuarios.service.UsuarioService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;

import org.springframework.security.core.Authentication;

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

    // PERFIL PROPIO

    @GetMapping("/me")
    public UsuarioResponse obtenerPerfil(
        Authentication authentication
    ) {

        Long id = Long.valueOf(
            authentication.getName()
        );

        return usuarioService.buscarPorId(id);
    }

    // BUSCAR USUARIO

    @GetMapping("/{id}")
    public UsuarioResponse buscarPorId(
        @PathVariable Long id
    ) {

        return usuarioService.buscarPorId(id);
    }

    // LISTAR PERSONAL

    @GetMapping("/personal")
    public List<UsuarioResponse> listarPersonal(

        @RequestParam(defaultValue = "")
        String query,

        @RequestParam(defaultValue = "20")
        int limit,

        @RequestParam(defaultValue = "0")
        int offset

    ) {

        return usuarioService.listarPersonal(
            query, limit, offset
        );
    }

    // REGISTRAR PERSONAL

    @PostMapping("/personal")
    @ResponseStatus(HttpStatus.CREATED)
    public PersonalCreadoResponse registrarPersonal(
        @Valid @RequestBody PersonalRequest request
    ) {

        return usuarioService.registrarPersonal(request);
    }

    // ACTUALIZAR PERSONAL

    @PutMapping("/{id}")
    public UsuarioResponse actualizarPersonal(

        @PathVariable Long id,

        @Valid @RequestBody PersonalRequest request

    ) {

        return usuarioService.actualizarPersonal(
            id,
            request
        );
    }

    // ELIMINAR PERSONAL

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarPersonal(
        @PathVariable Long id
    ) {

        usuarioService.eliminarPersonal(id);
    }

    // ACTUALIZAR FOTO PROPIA

    @PatchMapping("/me/foto")
    public UsuarioResponse actualizarFoto(

        Authentication authentication,

        @Valid @RequestBody FotoRequest request

    ) {

        Long id = Long.valueOf(
            authentication.getName()
        );

        return usuarioService.actualizarFoto(
            id,
            request
        );
    }

    // CAMBIAR CONTRASENA PROPIA

    @PatchMapping("/me/contrasena")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cambiarContrasena(

        Authentication authentication,

        @Valid @RequestBody
        CambiarContrasenaRequest request

    ) {

        Long id = Long.valueOf(
            authentication.getName()
        );

        usuarioService.cambiarContrasena(
            id,
            request
        );
    }
}