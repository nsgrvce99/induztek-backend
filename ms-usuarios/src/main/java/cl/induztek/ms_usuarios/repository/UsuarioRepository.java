package cl.induztek.ms_usuarios.repository;

import cl.induztek.ms_usuarios.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository
        extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCorreo(String correo);

    Optional<Usuario> findByRut(String rut);

    Optional<Usuario> findByCorreoAndActivoTrue(String correo);

    boolean existsByCorreo(String correo);

    boolean existsByRut(String rut);

    List<Usuario> findByRol_Id(Long rolId);

    Page<Usuario> findByRol_Id(
        Long rolId, Pageable pageable
    );

    Page<Usuario> findByRol_IdNot(
        Long rolId, Pageable pageable
    );
}
