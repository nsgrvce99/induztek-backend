package cl.induztek.ms_protocolos.repository;

import cl.induztek.ms_protocolos.model.Protocolo;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface ProtocoloRepository extends JpaRepository<Protocolo, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Protocolo> findByIdAndTecnicoId(long id, long tecnicoId);

    Page<Protocolo> findByEquipoId(long equipoId, Pageable pagina);
    Page<Protocolo> findByEquipoIdAndEstadoNotIn(long equipoId, java.util.Collection<Protocolo.Estado> estados, Pageable pagina);
    Page<Protocolo> findByTecnicoId(long tecnicoId, Pageable pagina);
    Page<Protocolo> findByEstado(Protocolo.Estado estado, Pageable pagina);
    @org.springframework.data.jpa.repository.Query("""
            select p.estado as estado, p.resultadoGlobal as resultado, count(p) as cantidad
            from Protocolo p where p.tecnicoId = :tecnicoId
            group by p.estado, p.resultadoGlobal
            """)
    java.util.List<Conteo> resumir(long tecnicoId);

    interface Conteo {
        Protocolo.Estado getEstado();
        cl.induztek.ms_protocolos.service.EvaluacionPrueba.Resultado getResultado();
        long getCantidad();
    }

    boolean existsByEquipoId(long equipoId);
    boolean existsByEquipoIdAndEstado(long equipoId, Protocolo.Estado estado);
    boolean existsByEquipoIdInAndEstado(java.util.Collection<Long> equipos, Protocolo.Estado estado);
    boolean existsByTecnicoId(long tecnicoId);
}
