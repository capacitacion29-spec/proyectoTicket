package com.example.ticketero.repository;

import com.example.ticketero.model.entity.Advisor;
import com.example.ticketero.model.enums.AdvisorStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdvisorRepository extends JpaRepository<Advisor, Long> {
    
    boolean existsByEmail(String email);
    
    List<Advisor> findByStatus(AdvisorStatus status);
    
    @Query("SELECT COUNT(a) FROM Advisor a WHERE a.status = :status")
    Long countByStatus(AdvisorStatus status);
    
    @Query("SELECT a FROM Advisor a WHERE a.status = 'AVAILABLE' ORDER BY a.assignedTicketsCount ASC LIMIT 1")
    Optional<Advisor> findAvailableAdvisorWithLeastTickets();
    
    @Query("SELECT MAX(a.moduleNumber) FROM Advisor a")
    Optional<Integer> findMaxModuleNumber();
}