-- Aplicar con un usuario administrador. Puede repetirse en volúmenes existentes.
USE induztek_usuarios;
SET @ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'usuario' AND column_name = 'archivado'),
    'SELECT 1 WHERE FALSE', 'ALTER TABLE usuario ADD COLUMN archivado BIT NOT NULL DEFAULT b''0''');
PREPARE cambio FROM @ddl;
EXECUTE cambio;
DEALLOCATE PREPARE cambio;

USE induztek_equipos;
SET @ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'instalacion' AND column_name = 'archivada'),
    'SELECT 1 WHERE FALSE', 'ALTER TABLE instalacion ADD COLUMN archivada BIT NOT NULL DEFAULT b''0''');
PREPARE cambio FROM @ddl;
EXECUTE cambio;
DEALLOCATE PREPARE cambio;
SET @ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'equipo' AND column_name = 'archivado'),
    'SELECT 1 WHERE FALSE', 'ALTER TABLE equipo ADD COLUMN archivado BIT NOT NULL DEFAULT b''0''');
PREPARE cambio FROM @ddl;
EXECUTE cambio;
DEALLOCATE PREPARE cambio;

USE induztek_protocolos;
SET @ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'protocolo' AND column_name = 'texto_mediciones'),
    'SELECT 1 WHERE FALSE', 'ALTER TABLE protocolo ADD COLUMN texto_mediciones JSON');
PREPARE cambio FROM @ddl;
EXECUTE cambio;
DEALLOCATE PREPARE cambio;

USE induztek_instrumentos;
UPDATE instrumento SET nombre = 'Megohmetro 5 kV'
WHERE id_instrumento = 1 AND numero_serie = 'MIT-0045821' AND nombre = 'Megóhmetro 5 kV';
UPDATE instrumento SET nombre = 'Megohmetro 10 kV'
WHERE id_instrumento = 2 AND numero_serie = 'MIT-0071233' AND nombre = 'Megóhmetro 10 kV';