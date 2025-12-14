package com.example.ticketero.model.entity;

import com.example.ticketero.model.enums.QueueType;
import com.example.ticketero.model.enums.TicketStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ticket")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticket {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "codigo_referencia", nullable = false, unique = true)
    private String codigoReferencia;
    
    @Column(name = "numero", nullable = false, unique = true, length = 10)
    private String numero;
    
    @Column(name = "national_id", nullable = false, length = 20)
    private String nationalId;
    
    @Column(name = "nombre_cliente", nullable = false, length = 100)
    private String nombreCliente;
    
    @Column(name = "telefono", length = 20)
    private String telefono;
    
    @Column(name = "branch_office", nullable = false, length = 100)
    @Builder.Default
    private String branchOffice = "Sucursal Principal";
    
    @Enumerated(EnumType.STRING)
    @Column(name = "queue_type", nullable = false)
    @Builder.Default
    private QueueType queueType = QueueType.CAJA;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private TicketStatus status = TicketStatus.EN_ESPERA;
    
    @Column(name = "position_in_queue", nullable = false)
    @Builder.Default
    private Integer positionInQueue = 0;
    
    @Column(name = "estimated_wait_minutes", nullable = false)
    @Builder.Default
    private Integer estimatedWaitMinutes = 0;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_advisor_id")
    private Advisor assignedAdvisor;
    
    @Column(name = "assigned_module_number")
    private Integer assignedModuleNumber;
    
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @PrePersist
    protected void onCreate() {
        if (codigoReferencia == null) {
            codigoReferencia = "TK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}