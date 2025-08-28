package org.hrachov.com.filmproject.controller;

import org.hrachov.com.filmproject.model.Serial;
import org.hrachov.com.filmproject.service.impl.SerialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
@RestController
@RequestMapping("/api/serial")
public class SerialApiController {

    private final SerialService serialService; // Используем SerialService вместо SerialRepository напрямую

    @Autowired
    public SerialApiController(SerialService serialService) { // Внедряем SerialService
        this.serialService = serialService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Serial> getSerialDetailsById(@PathVariable Long id) {
        // Вызываем метод сервиса
        Optional<Serial> serialOptional = serialService.getSerialDetails(id);

        return serialOptional.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
        // Или так, для большей наглядности:
        // if (serialOptional.isPresent()) {
        //     return ResponseEntity.ok(serialOptional.get());
        // } else {
        //     return ResponseEntity.notFound().build();
        // }
    }
}