package com.example.ticketero.model.enums;

import java.util.List;

/**
 * Estados posibles de un ticket
 */
public enum TicketStatus {
    EN_ESPERA,      // Esperando asignación
    PROXIMO,        // Próximo a ser atendido (posición <= 3)
    ATENDIENDO,     // Siendo atendido por un asesor
    COMPLETADO,     // Atención finalizada
    CANCELADO,      // Cancelado por cliente o sistema
    NO_ATENDIDO;    // Cliente no se presentó

    public static List<TicketStatus> getActiveStatuses() {
        return List.of(EN_ESPERA, PROXIMO, ATENDIENDO);
    }

    public boolean isActive() {
        return getActiveStatuses().contains(this);
    }
}