package cl.induztek.ms_usuarios.repository;

import cl.induztek.ms_usuarios.model.Usuario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

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

    boolean existsByRutAndIdNot(String rut, Long id);

    boolean existsByCorreoAndIdNot(String correo, Long id);

    List<Usuario> findByRol_Id(Long rolId);

    Page<Usuario> findByRol_Id(
        Long rolId, Pageable pageable
    );

    Page<Usuario> findByRol_IdNot(
        Long rolId, Pageable pageable
    );

    @Query(value = """
        SELECT u.* FROM usuario u
        WHERE u.id_rol = 4 AND u.archivado = false
        AND (
            u.nombre LIKE CONCAT('%', :query, '%')
            OR u.rut LIKE CONCAT('%', :rutQuery, '%')
            OR u.correo LIKE CONCAT('%', :query, '%')
        )
        ORDER BY u.nombre ASC, u.id_usuario ASC
        LIMIT :limite OFFSET :offset
        """, nativeQuery = true)
    List<Usuario> buscarClientes(
        @Param("query") String query,
        @Param("rutQuery") String rutQuery,
        @Param("limite") int limite,
        @Param("offset") int offset
    );

    @Query(value = """
        SELECT u.* FROM usuario u
        WHERE u.id_rol IN (1, 2, 3)
        AND (
            u.nombre LIKE CONCAT('%', :query, '%')
            OR u.rut LIKE CONCAT('%', :rutQuery, '%')
            OR u.correo LIKE CONCAT('%', :query, '%')
        )
        ORDER BY u.nombre ASC, u.id_usuario ASC
        LIMIT :limite OFFSET :offset
        """, nativeQuery = true)
    List<Usuario> buscarPersonal(
        @Param("query") String query,
        @Param("rutQuery") String rutQuery,
        @Param("limite") int limite,
        @Param("offset") int offset
    );
}