package cl.induztek.ms_equipos.repository;

import cl.induztek.ms_equipos.model.Equipo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EquipoRepository extends JpaRepository<Equipo, Long> {
    @Query("select e from Equipo e where e.instalacion.id = :idInstalacion and e.archivado = false")
    Page<Equipo> findByInstalacionId(Long idInstalacion, Pageable pageable);
    boolean existsByInstalacionId(Long idInstalacion);
    boolean existsByIdCliente(Long idCliente);
    boolean existsByInstalacionIdAndArchivadoFalse(Long idInstalacion);

    @Query("select e.id from Equipo e where e.idCliente = :idCliente")
    java.util.List<Long> idsDelCliente(Long idCliente);

    @Query("""
            select e from Equipo e
            where e.idCliente = :cliente and e.archivado = false
              and (lower(e.tag) like lower(concat('%', :busqueda, '%'))
                or lower(e.instalacion.nombre) like lower(concat('%', :busqueda, '%')))
            """)
    Page<Equipo> buscarDelCliente(@Param("cliente") Long cliente, @Param("busqueda") String busqueda,
                                 Pageable pageable);
}
