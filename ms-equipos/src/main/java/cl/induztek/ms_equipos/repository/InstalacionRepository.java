package cl.induztek.ms_equipos.repository;

import cl.induztek.ms_equipos.model.Instalacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstalacionRepository extends JpaRepository<Instalacion, Long> {
    Page<Instalacion> findByArchivadaFalseAndNombreContainingIgnoreCase(String nombre, Pageable pageable);
    boolean existsByIdCliente(Long idCliente);
}
