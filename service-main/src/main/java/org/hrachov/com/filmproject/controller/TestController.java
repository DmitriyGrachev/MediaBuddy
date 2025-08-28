package org.hrachov.com.filmproject.controller;

import org.hrachov.com.filmproject.model.dto.TestDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestController {

    private final NotificationWebSocketController notificationWebSocketController;
    public TestController(NotificationWebSocketController notificationWebSocketController) {
        this.notificationWebSocketController = notificationWebSocketController;
    }

    @GetMapping
    public ResponseEntity<Map<String,String>> test(@RequestBody String value) {
        Map<String,String> map = new HashMap<>();
        map.put("message", value != null ? "Received: " + value : "Test endpoint");
        map.put("status","succes");

        return new ResponseEntity<>(map,HttpStatus.OK);
    }
    @PostMapping
    public ResponseEntity<Map<String,String>> testPost(@RequestBody TestDto testDto) {
        if(testDto.getInput() == null || testDto.getInput().isEmpty()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        Map<String,String> map = new HashMap<>();
        map.put("message",testDto.getInput());
        map.put("status","success");

        return new ResponseEntity<>(map,HttpStatus.OK);
    }
    @GetMapping("/test-notification")
    public ResponseEntity<String> testNotification() {
        notificationWebSocketController.sendNotification("dmitry", "Test notification");
        return ResponseEntity.ok("Test notification sent");
    }
}
