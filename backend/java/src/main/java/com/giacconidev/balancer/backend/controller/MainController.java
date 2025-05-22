package com.giacconidev.balancer.backend.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.giacconidev.balancer.backend.dto.TaskDto;
import com.giacconidev.balancer.backend.service.BotService;
import com.giacconidev.balancer.backend.service.KafkaProducer;

@RestController
@Profile("!test") 
@RequestMapping("/api/v1")
public class MainController {

    private KafkaProducer kafkaProducer;
    private BotService botService;

    public MainController(KafkaProducer kafkaProducer, BotService botService) {
        this.kafkaProducer = kafkaProducer;
        this.botService = botService;
    }

    @PostMapping("/process/{botId}")
    public ResponseEntity<String> processInput(@PathVariable String botId, @RequestBody TaskDto input) {
        if(input.getActionType() == null || input.getActionType().isEmpty())
            return ResponseEntity.badRequest().body("Action is required");
        // Save the command to the bot
        botService.addNewTaskToBot(botId, input);
        // Send the command to the Kafka topic
        kafkaProducer.sendMessage(botId, input);
        return ResponseEntity.ok("Command sent");
    }
}