package com.example.ticketero.dto;

import com.example.ticketero.model.enums.AdvisorStatus;

/**
 * Response con información del asesor
 */
public record AdvisorResponse(
    Long id,
    String name,
    String email,
    AdvisorStatus status,
    Integer moduleNumber,
    Integer assignedTicketsCount,
    String currentTicket,
    Long currentTicketId
) {}