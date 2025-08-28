package org.hrachov.com.filmproject.controller;

import org.hrachov.com.filmproject.model.dto.ChatMessageDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/chat/")
public class ChatController {
    private final RestTemplate restTemplate = new RestTemplate();
    @PostMapping("/ask")
    public ResponseEntity<Map<String,String>> ask(@RequestBody ChatMessageDTO message) {
        // Адрес Python API
        String url = "https://2fa1-34-106-94-155.ngrok-free.app/api/chat/ask";

        // Отправляем POST-запрос и получаем Map<String, String> как ответ
        ResponseEntity<Map> response = restTemplate.postForEntity(url, message, Map.class);

        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }
}
