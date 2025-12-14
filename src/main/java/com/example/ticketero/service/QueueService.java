package com.example.ticketero.service;

import com.example.ticketero.dto.QueueStatusResponse;
import com.example.ticketero.dto.TicketResponse;
import com.example.ticketero.model.enums.TicketStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class QueueService {
    
    private final TicketService ticketService;
    private final AdvisorService advisorService;
    
    public QueueStatusResponse getQueueStatus() {
        log.info("Getting queue status");
        
        List<TicketResponse> activeTickets = ticketService.getActiveTickets();
        Long availableAdvisors = advisorService.getAvailableAdvisorsCount();
        Long busyAdvisors = advisorService.getBusyAdvisorsCount();
        
        // Obtener próximos 3 tickets
        List<TicketResponse> nextTickets = activeTickets.stream()
                .filter(ticket -> ticket.status() == TicketStatus.EN_ESPERA)
                .limit(3)
                .toList();
        
        // Calcular tiempo promedio de espera
        Integer averageWaitTime = calculateAverageWaitTime(activeTickets);
        
        return new QueueStatusResponse(
                activeTickets.size(),
                availableAdvisors.intValue(),
                busyAdvisors.intValue(),
                nextTickets,
                averageWaitTime
        );
    }
    
    private Integer calculateAverageWaitTime(List<TicketResponse> activeTickets) {
        if (activeTickets.isEmpty()) {
            return 0;
        }
        
        return activeTickets.stream()
                .mapToInt(TicketResponse::estimatedWaitMinutes)
                .sum() / activeTickets.size();
    }
}