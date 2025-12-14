package com.example.ticketero.service;

import com.example.ticketero.dto.CreateTicketRequest;
import com.example.ticketero.dto.TicketResponse;
import com.example.ticketero.model.entity.Ticket;
import com.example.ticketero.model.enums.QueueType;
import com.example.ticketero.model.enums.TicketStatus;
import com.example.ticketero.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketService {
    
    private final TicketRepository ticketRepository;
    private final MessageService messageService;
    private final AtomicInteger ticketCounter = new AtomicInteger(1);
    
    @Transactional
    public TicketResponse createTicket(CreateTicketRequest request) {
        log.info("Creating ticket for nationalId: {}", request.nationalId());
        
        if (ticketRepository.existsByNationalId(request.nationalId())) {
            throw new RuntimeException("Ya existe un ticket activo para este National ID");
        }
        
        QueueType queueType = QueueType.CAJA; // Por defecto CAJA
        String numero = generateTicketNumber(queueType);
        
        Ticket ticket = Ticket.builder()
                .nationalId(request.nationalId())
                .nombreCliente(request.nombreCliente())
                .telefono(request.telefono())
                .numero(numero)
                .queueType(queueType)
                .status(TicketStatus.EN_ESPERA)
                .positionInQueue(calculatePosition())
                .estimatedWaitMinutes(calculateEstimatedWait(queueType))
                .build();
        
        ticket = ticketRepository.save(ticket);
        
        // Programar mensaje de confirmación
        messageService.scheduleTicketCreatedMessage(ticket);
        
        log.info("Ticket created: {}", ticket.getCodigoReferencia());
        return mapToResponse(ticket);
    }
    
    public TicketResponse getTicketByCode(String codigoReferencia) {
        Ticket ticket = ticketRepository.findByCodigoReferencia(codigoReferencia)
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado"));
        return mapToResponse(ticket);
    }
    
    public List<TicketResponse> getActiveTickets() {
        List<TicketStatus> activeStatuses = TicketStatus.getActiveStatuses();
        return ticketRepository.findByStatusInOrderByCreatedAtAsc(activeStatuses)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }
    
    @Transactional
    public void updateTicketStatus(Long ticketId, TicketStatus newStatus) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado"));
        
        ticket.setStatus(newStatus);
        ticketRepository.save(ticket);
        
        log.info("Ticket {} status updated to {}", ticket.getCodigoReferencia(), newStatus);
    }
    
    private String generateTicketNumber(QueueType queueType) {
        return queueType.getPrefix() + String.format("%03d", ticketCounter.getAndIncrement());
    }
    
    private Integer calculatePosition() {
        Long activeCount = ticketRepository.countByStatusIn(TicketStatus.getActiveStatuses());
        return activeCount.intValue() + 1;
    }
    
    private Integer calculateEstimatedWait(QueueType queueType) {
        Integer position = calculatePosition();
        return position * queueType.getAvgTimeMinutes();
    }
    
    private TicketResponse mapToResponse(Ticket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getCodigoReferencia(),
                ticket.getNumero(),
                ticket.getNationalId(),
                ticket.getNombreCliente(),
                ticket.getTelefono(),
                ticket.getBranchOffice(),
                ticket.getQueueType(),
                ticket.getStatus(),
                ticket.getPositionInQueue(),
                ticket.getEstimatedWaitMinutes(),
                ticket.getAssignedAdvisor() != null ? ticket.getAssignedAdvisor().getName() : null,
                ticket.getAssignedModuleNumber(),
                ticket.getCreatedAt()
        );
    }
}