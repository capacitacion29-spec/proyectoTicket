package com.example.ticketero.repository;

import com.example.ticketero.model.entity.Ticket;
import com.example.ticketero.model.enums.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    
    Optional<Ticket> findByCodigoReferencia(String codigoReferencia);
    
    boolean existsByNationalId(String nationalId);
    
    List<Ticket> findByStatusInOrderByCreatedAtAsc(List<TicketStatus> statuses);
    
    @Query("SELECT t FROM Ticket t WHERE t.status IN :statuses ORDER BY t.createdAt ASC")
    List<Ticket> findActiveTicketsOrderedByCreation(List<TicketStatus> statuses);
    
    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.status IN :statuses")
    Long countByStatusIn(List<TicketStatus> statuses);
    
    @Query("SELECT t FROM Ticket t WHERE t.status = 'EN_ESPERA' ORDER BY t.createdAt ASC LIMIT 1")
    Optional<Ticket> findNextTicketToAssign();
}