package com.example.ticketero.model.enums;

/**
 * Estados de envío de mensajes
 */
public enum MessageStatus {
    PENDIENTE,  // Esperando ser enviado
    ENVIADO,    // Enviado exitosamente
    FALLIDO;    // Falló el envío
}