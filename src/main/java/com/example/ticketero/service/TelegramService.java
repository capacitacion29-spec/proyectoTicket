package com.example.ticketero.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
@Slf4j
public class TelegramService {
    
    @Value("${telegram.bot.token}")
    private String botToken;
    
    @Value("${telegram.bot.chat-id}")
    private String chatId;
    
    @Value("${telegram.bot.api-url:https://api.telegram.org}")
    private String apiUrl;
    
    private final WebClient webClient;
    
    public TelegramService() {
        this.webClient = WebClient.builder()
            .baseUrl("https://api.telegram.org")
            .build();
    }
    
    public boolean sendMessage(String messageText) {
        try {
            log.info("📱 Sending Telegram message to chat: {}", chatId);
            log.info("Message: {}", messageText);
            
            String response = webClient.post()
                .uri("/bot{token}/sendMessage", botToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                    "chat_id", chatId,
                    "text", messageText,
                    "parse_mode", "HTML"
                ))
                .retrieve()
                .bodyToMono(String.class)
                .block();
                
            log.info("✅ Telegram message sent successfully");
            log.debug("Telegram API response: {}", response);
            return true;
            
        } catch (Exception e) {
            log.error("❌ Failed to send Telegram message: {}", e.getMessage());
            log.debug("Error details:", e);
            return false;
        }
    }
}