package com.example.ticketero.dto;

import com.example.ticketero.model.enums.QueueType;
import com.example.ticketero.model.enums.TicketStatus;

import java.time.LocalDateTime;

/**
 * Response con información del ticket
 */
public record TicketResponse(
    Long id,
    String codigoReferencia,
    String numero,
    String nationalId,
    String nombreCliente,
    String telefono,
    String branchOffice,
    QueueType queueType,
    TicketStatus status,
    Integer positionInQueue,
    Integer estimatedWaitMinutes,
    String assignedAdvisorName,
    Integer assignedModuleNumber,
    LocalDateTime createdAt
) {}