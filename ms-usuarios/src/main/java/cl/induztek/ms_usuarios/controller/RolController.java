package cl.induztek.ms_usuarios.controller;

import cl.induztek.ms_usuarios.model.Rol;
import cl.induztek.ms_usuarios.repository.RolRepository;

import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Sort;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
public class RolController {

    private final RolRepository rolRepository;

    public RolController(
        RolRepository rolRepository
    ) {
        this.rolRepository = rolRepository;
    }

    @GetMapping
    public List<Rol> listarRoles() {

        return rolRepository.findAll(
            Sort.by("id")
        );
    }
}