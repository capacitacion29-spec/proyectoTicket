-- V3__create_advisor_table.sql
-- Tabla de asesores/ejecutivos

CREATE TABLE advisor (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    module_number INTEGER NOT NULL,
    assigned_tickets_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_module_number CHECK (module_number BETWEEN 1 AND 5),
    CONSTRAINT chk_assigned_count CHECK (assigned_tickets_count >= 0)
);

-- Índice para búsqueda de asesores disponibles
CREATE INDEX idx_advisor_status ON advisor(status);
CREATE INDEX idx_advisor_module ON advisor(module_number);

-- Foreign key de ticket a advisor (se agrega ahora que advisor existe)
ALTER TABLE ticket
    ADD CONSTRAINT fk_ticket_advisor 
    FOREIGN KEY (assigned_advisor_id) 
    REFERENCES advisor(id) 
    ON DELETE SET NULL;

-- Datos iniciales: 5 asesores
INSERT INTO advisor (name, email, status, module_number) VALUES
    ('María González', 'maria.gonzalez@institucion.cl', 'AVAILABLE', 1),
    ('Juan Pérez', 'juan.perez@institucion.cl', 'AVAILABLE', 2),
    ('Ana Silva', 'ana.silva@institucion.cl', 'AVAILABLE', 3),
    ('Carlos Rojas', 'carlos.rojas@institucion.cl', 'AVAILABLE', 4),
    ('Patricia Díaz', 'patricia.diaz@institucion.cl', 'AVAILABLE', 5);

-- Comentarios
COMMENT ON TABLE advisor IS 'Asesores/ejecutivos que atienden clientes';
COMMENT ON COLUMN advisor.status IS 'Estado: AVAILABLE, BUSY, OFFLINE';
COMMENT ON COLUMN advisor.module_number IS 'Número de módulo de atención (1-5)';
COMMENT ON COLUMN advisor.assigned_tickets_count IS 'Cantidad de tickets actualmente asignados';