package com.example.ticketero.service;

import com.example.ticketero.model.entity.Mensaje;
import com.example.ticketero.model.entity.Ticket;
import com.example.ticketero.model.enums.MessageStatus;
import com.example.ticketero.model.enums.MessageTemplate;
import com.example.ticketero.repository.MensajeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {
    
    private final MensajeRepository mensajeRepository;
    private final TelegramService telegramService;
    
    @Transactional
    public void scheduleTicketCreatedMessage(Ticket ticket) {
        log.info("Scheduling ticket created message for: {}", ticket.getCodigoReferencia());
        
        Mensaje mensaje = Mensaje.builder()
                .ticket(ticket)
                .plantilla(MessageTemplate.TOTEM_TICKET_CREADO)
                .estadoEnvio(MessageStatus.PENDIENTE)
                .fechaProgramada(LocalDateTime.now().plusSeconds(5)) // Enviar en 5 segundos
                .intentos(0)
                .build();
        
        mensajeRepository.save(mensaje);
    }
    
    @Transactional
    public void scheduleYourTurnMessage(Ticket ticket) {
        log.info("Scheduling your turn message for: {}", ticket.getCodigoReferencia());
        
        Mensaje mensaje = Mensaje.builder()
                .ticket(ticket)
                .plantilla(MessageTemplate.TOTEM_ES_TU_TURNO)
                .estadoEnvio(MessageStatus.PENDIENTE)
                .fechaProgramada(LocalDateTime.now().plusSeconds(2)) // Enviar inmediatamente
                .intentos(0)
                .build();
        
        mensajeRepository.save(mensaje);
    }
    
    @Transactional
    public void scheduleProximoTurnoMessage(Ticket ticket) {
        log.info("Scheduling proximo turno message for: {}", ticket.getCodigoReferencia());
        
        Mensaje mensaje = Mensaje.builder()
                .ticket(ticket)
                .plantilla(MessageTemplate.TOTEM_PROXIMO_TURNO)
                .estadoEnvio(MessageStatus.PENDIENTE)
                .fechaProgramada(LocalDateTime.now().plusSeconds(3))
                .intentos(0)
                .build();
        
        mensajeRepository.save(mensaje);
    }
    
    @Transactional
    public void processPendingMessages() {
        List<Mensaje> pendingMessages = mensajeRepository.findPendingMessagesToSend(LocalDateTime.now());
        
        log.info("Processing {} pending messages", pendingMessages.size());
        
        for (Mensaje mensaje : pendingMessages) {
            try {
                String messageText = buildMessageText(mensaje);
                boolean sent = telegramService.sendMessage(messageText);
                
                if (sent) {
                    mensaje.setEstadoEnvio(MessageStatus.ENVIADO);
                    mensaje.setFechaEnvio(LocalDateTime.now());
                    log.info("Message sent successfully for ticket: {}", mensaje.getTicket().getCodigoReferencia());
                } else {
                    mensaje.setIntentos(mensaje.getIntentos() + 1);
                    if (mensaje.getIntentos() >= 3) {
                        mensaje.setEstadoEnvio(MessageStatus.FALLIDO);
                        log.error("Message failed after 3 attempts for ticket: {}", mensaje.getTicket().getCodigoReferencia());
                    }
                }
                
                mensajeRepository.save(mensaje);
                
            } catch (Exception e) {
                log.error("Error processing message for ticket: {}", mensaje.getTicket().getCodigoReferencia(), e);
                mensaje.setIntentos(mensaje.getIntentos() + 1);
                if (mensaje.getIntentos() >= 3) {
                    mensaje.setEstadoEnvio(MessageStatus.FALLIDO);
                }
                mensajeRepository.save(mensaje);
            }
        }
    }
    
    private String buildMessageText(Mensaje mensaje) {
        Ticket ticket = mensaje.getTicket();
        
        return switch (mensaje.getPlantilla()) {
            case TOTEM_TICKET_CREADO -> String.format(
                    "🎫 *Ticket Creado*\n\n" +
                    "Código: `%s`\n" +
                    "Número: *%s*\n" +
                    "Cliente: %s\n" +
                    "Posición en cola: %d\n" +
                    "Tiempo estimado: %d minutos\n\n" +
                    "¡Gracias por su paciencia!",
                    ticket.getCodigoReferencia(),
                    ticket.getNumero(),
                    ticket.getNombreCliente(),
                    ticket.getPositionInQueue(),
                    ticket.getEstimatedWaitMinutes()
            );
            
            case TOTEM_ES_TU_TURNO -> String.format(
                    "🔔 *¡Es tu turno!*\n\n" +
                    "Ticket: *%s*\n" +
                    "Cliente: %s\n" +
                    "Módulo: *%d*\n" +
                    "Asesor: %s\n\n" +
                    "Por favor, diríjase al módulo indicado.",
                    ticket.getNumero(),
                    ticket.getNombreCliente(),
                    ticket.getAssignedModuleNumber(),
                    ticket.getAssignedAdvisor().getName()
            );
            
            case TOTEM_PROXIMO_TURNO -> String.format(
                    "⏰ *Próximo turno*\n\n" +
                    "Ticket: *%s*\n" +
                    "Cliente: %s\n" +
                    "Posición: %d\n\n" +
                    "Prepárese, será atendido pronto.",
                    ticket.getNumero(),
                    ticket.getNombreCliente(),
                    ticket.getPositionInQueue()
            );
        };
    }
}