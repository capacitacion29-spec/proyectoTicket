package com.example.ticketero.service;

import com.example.ticketero.dto.AdvisorResponse;
import com.example.ticketero.dto.CreateAdvisorRequest;
import com.example.ticketero.model.entity.Advisor;
import com.example.ticketero.model.entity.Ticket;
import com.example.ticketero.model.enums.AdvisorStatus;
import com.example.ticketero.model.enums.TicketStatus;
import com.example.ticketero.repository.AdvisorRepository;
import com.example.ticketero.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdvisorService {
    
    private final AdvisorRepository advisorRepository;
    private final TicketRepository ticketRepository;
    private final MessageService messageService;
    
    @Transactional
    public AdvisorResponse createAdvisor(CreateAdvisorRequest request) {
        log.info("Creating advisor: {}", request.email());
        
        if (advisorRepository.existsByEmail(request.email())) {
            throw new RuntimeException("Ya existe un asesor con este email");
        }
        
        Integer nextModuleNumber = advisorRepository.findMaxModuleNumber()
                .map(max -> max + 1)
                .orElse(1);
        
        Advisor advisor = Advisor.builder()
                .name(request.nombre())
                .email(request.email())
                .moduleNumber(nextModuleNumber)
                .status(AdvisorStatus.AVAILABLE)
                .assignedTicketsCount(0)
                .build();
        
        advisor = advisorRepository.save(advisor);
        log.info("Advisor created: {} - Module {}", advisor.getName(), advisor.getModuleNumber());
        
        return mapToResponse(advisor);
    }
    
    public List<AdvisorResponse> getAllAdvisors() {
        return advisorRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }
    
    @Transactional
    public Optional<AdvisorResponse> assignNextTicket() {
        log.info("Attempting to assign next ticket");
        
        Optional<Advisor> availableAdvisor = advisorRepository.findAvailableAdvisorWithLeastTickets();
        Optional<Ticket> nextTicket = ticketRepository.findNextTicketToAssign();
        
        if (availableAdvisor.isEmpty() || nextTicket.isEmpty()) {
            log.info("No available advisor or no tickets to assign");
            return Optional.empty();
        }
        
        Advisor advisor = availableAdvisor.get();
        Ticket ticket = nextTicket.get();
        
        // Asignar ticket al asesor
        ticket.setAssignedAdvisor(advisor);
        ticket.setAssignedModuleNumber(advisor.getModuleNumber());
        ticket.setStatus(TicketStatus.ATENDIENDO);
        
        // Actualizar asesor
        advisor.setStatus(AdvisorStatus.BUSY);
        advisor.setAssignedTicketsCount(advisor.getAssignedTicketsCount() + 1);
        
        ticketRepository.save(ticket);
        advisorRepository.save(advisor);
        
        // Programar mensaje de notificación
        messageService.scheduleYourTurnMessage(ticket);
        
        log.info("Ticket {} assigned to advisor {} at module {}", 
                ticket.getCodigoReferencia(), advisor.getName(), advisor.getModuleNumber());
        
        return Optional.of(mapToResponse(advisor));
    }
    
    @Transactional
    public void completeTicketAttention(Long ticketId) {
        log.info("Completing attention for ticket ID: {}", ticketId);
        
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket no encontrado"));
        
        if (ticket.getAssignedAdvisor() == null) {
            throw new RuntimeException("Ticket no está asignado a ningún asesor");
        }
        
        Advisor advisor = ticket.getAssignedAdvisor();
        
        // Completar ticket
        ticket.setStatus(TicketStatus.COMPLETADO);
        
        // Liberar asesor
        advisor.setStatus(AdvisorStatus.AVAILABLE);
        advisor.setAssignedTicketsCount(Math.max(0, advisor.getAssignedTicketsCount() - 1));
        
        ticketRepository.save(ticket);
        advisorRepository.save(advisor);
        
        log.info("Ticket {} completed by advisor {}", ticket.getCodigoReferencia(), advisor.getName());
    }
    
    public Long getAvailableAdvisorsCount() {
        return advisorRepository.countByStatus(AdvisorStatus.AVAILABLE);
    }
    
    public Long getBusyAdvisorsCount() {
        return advisorRepository.countByStatus(AdvisorStatus.BUSY);
    }
    
    private AdvisorResponse mapToResponse(Advisor advisor) {
        String currentTicket = null;
        Long currentTicketId = null;
        
        if (advisor.getStatus() == AdvisorStatus.BUSY) {
            Optional<Ticket> assignedTicket = ticketRepository.findByAssignedAdvisorAndStatus(
                advisor, TicketStatus.ATENDIENDO);
            if (assignedTicket.isPresent()) {
                currentTicket = assignedTicket.get().getNumero();
                currentTicketId = assignedTicket.get().getId();
            }
        }
        
        return new AdvisorResponse(
                advisor.getId(),
                advisor.getName(),
                advisor.getEmail(),
                advisor.getStatus(),
                advisor.getModuleNumber(),
                advisor.getAssignedTicketsCount(),
                currentTicket,
                currentTicketId
        );
    }
}