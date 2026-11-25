CREATE DATABASE IF NOT EXISTS induztek_protocolos CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE induztek_protocolos;

CREATE TABLE IF NOT EXISTS protocolo (
    id_protocolo BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    version BIGINT NOT NULL,
    id_equipo BIGINT NOT NULL,
    id_usuario_registra BIGINT NOT NULL,
    fecha_hora DATETIME(6) NOT NULL,
    temperatura DECIMAL(8,3) NOT NULL,
    humedad DECIMAL(6,3) NOT NULL,
    resultado_global VARCHAR(32) NOT NULL,
    estado VARCHAR(24) NOT NULL,
    motivo_observacion VARCHAR(1000),
    comentario_variacion VARCHAR(1000),
    texto_mediciones JSON,
    INDEX ix_protocolo_equipo (id_equipo, id_protocolo),
    INDEX ix_protocolo_tecnico (id_usuario_registra, id_protocolo)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS medicion (
    id_medicion BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    id_protocolo BIGINT NOT NULL,
    tipo_prueba VARCHAR(24) NOT NULL,
    id_instrumento BIGINT NOT NULL,
    lectura_1min DECIMAL(24,8),
    lectura_10min DECIMAL(24,8),
    indice_polarizacion DECIMAL(24,8),
    corriente_ajuste DECIMAL(24,8),
    corriente_operacion DECIMAL(24,8),
    tiempo_esperado DECIMAL(24,8),
    tiempo_medido DECIMAL(24,8),
    lectura_devanado_a DECIMAL(24,8),
    lectura_devanado_b DECIMAL(24,8),
    lectura_devanado_c DECIMAL(24,8),
    CONSTRAINT fk_medicion_protocolo FOREIGN KEY (id_protocolo) REFERENCES protocolo (id_protocolo),
    INDEX ix_medicion_instrumento (id_instrumento)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS recepcion_acta (
    id_recepcion BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    id_protocolo BIGINT NOT NULL,
    nombre_receptor VARCHAR(120) NOT NULL,
    rut_receptor VARCHAR(16) NOT NULL,
    cargo_receptor VARCHAR(120) NOT NULL,
    fecha_hora_recepcion DATETIME(6) NOT NULL,
    CONSTRAINT uk_recepcion_protocolo UNIQUE (id_protocolo),
    CONSTRAINT fk_recepcion_protocolo FOREIGN KEY (id_protocolo) REFERENCES protocolo (id_protocolo)
) ENGINE=InnoDB;
