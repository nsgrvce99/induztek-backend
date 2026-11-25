package cl.induztek.ms_protocolos.repository;

import cl.induztek.ms_protocolos.model.Medicion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicionRepository extends JpaRepository<Medicion, Long> {
    java.util.Optional<Medicion> findFirstByProtocoloEquipoIdAndTipoPruebaAndProtocoloIdLessThanOrderByProtocoloIdDescIdAsc(
            long equipoId, cl.induztek.ms_protocolos.service.EvaluacionPrueba.Tipo tipo, long protocoloId);
    boolean existsByInstrumentoId(long instrumentoId);
}
