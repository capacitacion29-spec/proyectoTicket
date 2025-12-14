package com.example.ticketero.controller;

import com.example.ticketero.dto.AdvisorResponse;
import com.example.ticketero.dto.CreateAdvisorRequest;
import com.example.ticketero.service.AdvisorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/advisors")
@RequiredArgsConstructor
@Slf4j
public class AdvisorController {
    
    private final AdvisorService advisorService;
    
    @PostMapping
    public ResponseEntity<AdvisorResponse> createAdvisor(@Valid @RequestBody CreateAdvisorRequest request) {
        log.info("POST /api/advisors - Creating advisor: {}", request.email());
        
        try {
            AdvisorResponse response = advisorService.createAdvisor(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            log.error("Error creating advisor: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping
    public ResponseEntity<List<AdvisorResponse>> getAllAdvisors() {
        log.info("GET /api/advisors - Getting all advisors");
        
        List<AdvisorResponse> advisors = advisorService.getAllAdvisors();
        return ResponseEntity.ok(advisors);
    }
    
    @PostMapping("/assign-next")
    public ResponseEntity<AdvisorResponse> assignNextTicket() {
        log.info("POST /api/advisors/assign-next - Assigning next ticket");
        
        Optional<AdvisorResponse> assignedAdvisor = advisorService.assignNextTicket();
        
        if (assignedAdvisor.isPresent()) {
            return ResponseEntity.ok(assignedAdvisor.get());
        } else {
            log.info("No tickets to assign or no available advisors");
            return ResponseEntity.noContent().build();
        }
    }
    
    @PostMapping("/complete/{ticketId}")
    public ResponseEntity<Void> completeTicketAttention(@PathVariable Long ticketId) {
        log.info("POST /api/advisors/complete/{} - Completing ticket attention", ticketId);
        
        try {
            advisorService.completeTicketAttention(ticketId);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            log.error("Error completing ticket attention: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}