package cl.induztek.ms_instrumentos.repository;

import cl.induztek.ms_instrumentos.model.Instrumento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstrumentoRepository extends JpaRepository<Instrumento, Long> {
    Page<Instrumento> findByNombreContainingIgnoreCaseOrMarcaContainingIgnoreCase(
            String nombre, String marca, Pageable pagina);
}
