package cl.induztek.ms_protocolos.repository;

import cl.induztek.ms_protocolos.model.RecepcionActa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RecepcionActaRepository extends JpaRepository<RecepcionActa, Long> {
    Optional<RecepcionActa> findByProtocoloId(long protocoloId);
}
