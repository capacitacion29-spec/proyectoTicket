package com.example.ticketero.dto;

import java.util.List;

/**
 * Response con el estado actual de la cola
 */
public record QueueStatusResponse(
    Integer totalTicketsInQueue,
    Integer availableAdvisors,
    Integer busyAdvisors,
    List<TicketResponse> nextTickets,
    Integer averageWaitTimeMinutes
) {}