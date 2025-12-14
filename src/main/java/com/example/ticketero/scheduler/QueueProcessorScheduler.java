package com.example.ticketero.scheduler;

import com.example.ticketero.service.AdvisorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class QueueProcessorScheduler {
    
    private final AdvisorService advisorService;
    
    /**
     * Intenta asignar tickets automáticamente cada 5 segundos
     */
    @Scheduled(fixedRate = 5000) // 5 segundos
    public void processQueue() {
        log.debug("🎯 Processing queue for automatic assignment...");
        
        try {
            advisorService.assignNextTicket();
        } catch (Exception e) {
            log.error("Error processing queue", e);
        }
    }
}