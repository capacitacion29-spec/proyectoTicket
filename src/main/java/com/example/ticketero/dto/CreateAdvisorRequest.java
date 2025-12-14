package com.example.ticketero.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request para crear un nuevo asesor
 */
public record CreateAdvisorRequest(
    @NotBlank(message = "Nombre es requerido")
    @Size(max = 100, message = "Nombre no puede exceder 100 caracteres")
    String nombre,
    
    @NotBlank(message = "Email es requerido")
    @Email(message = "Email debe tener formato válido")
    @Size(max = 100, message = "Email no puede exceder 100 caracteres")
    String email
) {}