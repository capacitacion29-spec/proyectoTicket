-- V2__create_mensaje_table.sql
-- Tabla de mensajes programados para Telegram

CREATE TABLE mensaje (
    id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL,
    plantilla VARCHAR(50) NOT NULL,
    estado_envio VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    fecha_programada TIMESTAMP NOT NULL,
    fecha_envio TIMESTAMP,
    telegram_message_id VARCHAR(50),
    intentos INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_mensaje_ticket 
        FOREIGN KEY (ticket_id) 
        REFERENCES ticket(id) 
        ON DELETE CASCADE
);

-- Índices para performance del scheduler
CREATE INDEX idx_mensaje_estado_fecha ON mensaje(estado_envio, fecha_programada);
CREATE INDEX idx_mensaje_ticket_id ON mensaje(ticket_id);

-- Comentarios
COMMENT ON TABLE mensaje IS 'Mensajes programados para envío vía Telegram';
COMMENT ON COLUMN mensaje.plantilla IS 'Tipo de mensaje: totem_ticket_creado, totem_proximo_turno, totem_es_tu_turno';
COMMENT ON COLUMN mensaje.estado_envio IS 'Estado: PENDIENTE, ENVIADO, FALLIDO';
COMMENT ON COLUMN mensaje.intentos IS 'Cantidad de reintentos de envío';