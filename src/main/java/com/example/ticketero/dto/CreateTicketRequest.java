package com.example.ticketero.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request para crear un nuevo ticket
 */
public record CreateTicketRequest(
    @NotBlank(message = "National ID es requerido")
    @Size(max = 20, message = "National ID no puede exceder 20 caracteres")
    String nationalId,
    
    @NotBlank(message = "Nombre del cliente es requerido")
    @Size(max = 100, message = "Nombre no puede exceder 100 caracteres")
    String nombreCliente,
    
    @Size(max = 20, message = "Teléfono no puede exceder 20 caracteres")
    String telefono
) {}