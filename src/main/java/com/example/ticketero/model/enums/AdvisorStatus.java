package com.example.ticketero.model.enums;

/**
 * Estados posibles de un asesor
 */
public enum AdvisorStatus {
    AVAILABLE,  // Disponible para atender
    BUSY,       // Atendiendo un cliente
    OFFLINE;    // No disponible

    public boolean canReceiveAssignments() {
        return this == AVAILABLE;
    }
}