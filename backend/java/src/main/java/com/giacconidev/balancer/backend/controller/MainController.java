package com.giacconidev.balancer.backend.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.giacconidev.balancer.backend.dto.TaskDto;
import com.giacconidev.balancer.backend.service.KafkaProducer;

@RestController
@Profile("!test") 
@RequestMapping("/api/v1")
public class MainController {

    private KafkaProducer kafkaProducer;

    public MainController(KafkaProducer kafkaProducer) {
        this.kafkaProducer = kafkaProducer;
    }

    @PostMapping("/process/{botId}")
    public ResponseEntity<String> processInput(@PathVariable String botId, @RequestBody TaskDto input) {
        if(input.getTaskType() == null || input.getTaskType().isEmpty())
            return ResponseEntity.badRequest().body("Action is required");
        
        kafkaProducer.sendMessage(input);
        return ResponseEntity.ok("Command sent");
    }
}