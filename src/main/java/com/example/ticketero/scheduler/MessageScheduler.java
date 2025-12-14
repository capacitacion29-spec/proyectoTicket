package com.example.ticketero.scheduler;

import com.example.ticketero.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MessageScheduler {
    
    private final MessageService messageService;
    
    /**
     * Procesa mensajes pendientes cada 60 segundos
     */
    @Scheduled(fixedRate = 60000) // 60 segundos
    public void processPendingMessages() {
        log.debug("🔄 Processing pending messages...");
        
        try {
            messageService.processPendingMessages();
        } catch (Exception e) {
            log.error("Error processing pending messages", e);
        }
    }
}