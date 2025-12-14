package com.example.ticketero.controller;

import com.example.ticketero.service.TelegramService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telegram")
@RequiredArgsConstructor
public class TelegramTestController {
    
    private final TelegramService telegramService;
    
    @PostMapping("/test")
    public String testTelegram(@RequestParam String message) {
        boolean sent = telegramService.sendMessage("🧪 TEST: " + message);
        return sent ? "✅ Message sent successfully" : "❌ Failed to send message";
    }
}