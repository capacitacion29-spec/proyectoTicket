package com.example.ticketero.config;

import com.example.ticketero.model.entity.Advisor;
import com.example.ticketero.model.enums.AdvisorStatus;
import com.example.ticketero.repository.AdvisorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements CommandLineRunner {
    
    private final AdvisorRepository advisorRepository;
    
    @Override
    public void run(String... args) {
        if (advisorRepository.count() == 0) {
            log.info("🔄 Loading initial advisors data...");
            
            advisorRepository.save(Advisor.builder()
                .name("María González")
                .email("maria.gonzalez@institucion.cl")
                .status(AdvisorStatus.AVAILABLE)
                .moduleNumber(1)
                .build());
                
            advisorRepository.save(Advisor.builder()
                .name("Juan Pérez")
                .email("juan.perez@institucion.cl")
                .status(AdvisorStatus.AVAILABLE)
                .moduleNumber(2)
                .build());
                
            advisorRepository.save(Advisor.builder()
                .name("Ana Silva")
                .email("ana.silva@institucion.cl")
                .status(AdvisorStatus.AVAILABLE)
                .moduleNumber(3)
                .build());
                
            advisorRepository.save(Advisor.builder()
                .name("Carlos Rojas")
                .email("carlos.rojas@institucion.cl")
                .status(AdvisorStatus.AVAILABLE)
                .moduleNumber(4)
                .build());
                
            advisorRepository.save(Advisor.builder()
                .name("Patricia Díaz")
                .email("patricia.diaz@institucion.cl")
                .status(AdvisorStatus.AVAILABLE)
                .moduleNumber(5)
                .build());
                
            log.info("✅ Initial advisors loaded successfully");
        }
    }
}