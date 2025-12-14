package com.example.ticketero.controller;

import com.example.ticketero.dto.CreateTicketRequest;
import com.example.ticketero.dto.QueueStatusResponse;
import com.example.ticketero.dto.TicketResponse;
import com.example.ticketero.service.QueueService;
import com.example.ticketero.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class TicketController {
    
    private final TicketService ticketService;
    private final QueueService queueService;
    
    @PostMapping("/tickets")
    public ResponseEntity<TicketResponse> createTicket(@Valid @RequestBody CreateTicketRequest request) {
        log.info("POST /api/tickets - Creating ticket for: {}", request.nationalId());
        
        try {
            TicketResponse response = ticketService.createTicket(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            log.error("Error creating ticket: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/tickets/{codigoReferencia}")
    public ResponseEntity<TicketResponse> getTicket(@PathVariable String codigoReferencia) {
        log.info("GET /api/tickets/{} - Getting ticket", codigoReferencia);
        
        try {
            TicketResponse response = ticketService.getTicketByCode(codigoReferencia);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Ticket not found: {}", codigoReferencia);
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/queue/status")
    public ResponseEntity<QueueStatusResponse> getQueueStatus() {
        log.info("GET /api/queue/status - Getting queue status");
        
        QueueStatusResponse response = queueService.getQueueStatus();
        return ResponseEntity.ok(response);
    }
}