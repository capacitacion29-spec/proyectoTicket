package com.example.ticketero.model.entity;

import com.example.ticketero.model.enums.AdvisorStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "advisor")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Advisor {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "name", nullable = false, length = 100)
    private String name;
    
    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private AdvisorStatus status = AdvisorStatus.AVAILABLE;
    
    @Column(name = "module_number", nullable = false)
    private Integer moduleNumber;
    
    @Column(name = "assigned_tickets_count", nullable = false)
    @Builder.Default
    private Integer assignedTicketsCount = 0;
    
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @OneToMany(mappedBy = "assignedAdvisor", fetch = FetchType.LAZY)
    private List<Ticket> assignedTickets;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}