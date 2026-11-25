package cl.induztek.comun;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/** Coordina las mutaciones de referencias entre los esquemas de una misma instancia MySQL. */
public final class CoordinacionReferencias {
    private final DataSource dataSource;
    private final TransactionTemplate transaccion;
    private final ReentrantLock turnoLocal = new ReentrantLock();

    public CoordinacionReferencias(DataSource dataSource, PlatformTransactionManager transacciones) {
        this.dataSource = dataSource;
        this.transaccion = new TransactionTemplate(transacciones);
    }

    public <T> T ejecutar(Supplier<T> operacion) {
        try {
            if (!turnoLocal.tryLock(10, TimeUnit.SECONDS)) throw ocupado();
            try (Connection conexion = dataSource.getConnection()) {
                String motor = conexion.getMetaData().getDatabaseProductName();
                if ("H2".equals(motor)) return transaccion.execute(estado -> operacion.get());
                if (!"MySQL".equals(motor) && !"MariaDB".equals(motor)) throw ocupado();
                if (!ejecutarBloqueo(conexion, "SELECT GET_LOCK('induztek.referencias', 10)")) throw ocupado();
                try {
                    return transaccion.execute(estado -> operacion.get());
                } finally {
                    try {
                        ejecutarBloqueo(conexion, "SELECT RELEASE_LOCK('induztek.referencias')");
                    } catch (SQLException error) {
                        // Cerrar la conexión real evita devolver al pool una sesión que conserva el bloqueo.
                        conexion.abort(Runnable::run);
                    }
                }
            } finally {
                turnoLocal.unlock();
            }
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw ocupado();
        } catch (SQLException error) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo coordinar la operación", error);
        }
    }

    private static boolean ejecutarBloqueo(Connection conexion, String sql) throws SQLException {
        try (var consulta = conexion.createStatement(); var resultado = consulta.executeQuery(sql)) {
            return resultado.next() && resultado.getInt(1) == 1;
        }
    }

    private static ResponseStatusException ocupado() {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Otra operación está cambiando las referencias. Intenta nuevamente");
    }
}