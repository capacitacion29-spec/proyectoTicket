package com.example.ticketero.model.enums;

/**
 * Plantillas de mensajes para Telegram
 */
public enum MessageTemplate {
    TOTEM_TICKET_CREADO("totem_ticket_creado"),
    TOTEM_PROXIMO_TURNO("totem_proximo_turno"),
    TOTEM_ES_TU_TURNO("totem_es_tu_turno");

    private final String templateName;

    MessageTemplate(String templateName) {
        this.templateName = templateName;
    }

    public String getTemplateName() {
        return templateName;
    }
}