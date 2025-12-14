package com.example.ticketero.repository;

import com.example.ticketero.model.entity.Mensaje;
import com.example.ticketero.model.enums.MessageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MensajeRepository extends JpaRepository<Mensaje, Long> {
    
    @Query("SELECT m FROM Mensaje m WHERE m.estadoEnvio = 'PENDIENTE' AND m.fechaProgramada <= :now ORDER BY m.fechaProgramada ASC")
    List<Mensaje> findPendingMessagesToSend(LocalDateTime now);
    
    List<Mensaje> findByEstadoEnvioAndIntentosLessThan(MessageStatus estadoEnvio, Integer maxIntentos);
}