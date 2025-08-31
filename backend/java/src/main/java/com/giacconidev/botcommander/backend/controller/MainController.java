package com.giacconidev.botcommander.backend.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.giacconidev.botcommander.backend.dto.ActionDto;
import com.giacconidev.botcommander.backend.dto.BotDto;
import com.giacconidev.botcommander.backend.dto.ProcessDto;
import com.giacconidev.botcommander.backend.dto.TaskDto;
import com.giacconidev.botcommander.backend.model.Bot;
import com.giacconidev.botcommander.backend.service.BotCommanderSocketHandler;
import com.giacconidev.botcommander.backend.service.BotService;
import com.giacconidev.botcommander.backend.service.KafkaProducer;
import com.giacconidev.botcommander.backend.service.ProcessService;

import org.springframework.http.MediaType;

@RestController
@Profile("!test")
@RequestMapping("/api/v1")
public class MainController {

    private KafkaProducer kafkaProducer;
    private BotService botService;
    private ProcessService processService;
    private BotCommanderSocketHandler botCommanderSocketHandler;

    public MainController(KafkaProducer kafkaProducer, BotService botService,
            BotCommanderSocketHandler botCommanderSocketHandler,
            ProcessService processService) {
        this.kafkaProducer = kafkaProducer;
        this.botService = botService;
        this.botCommanderSocketHandler = botCommanderSocketHandler;
        this.processService = processService;
    }

    @PostMapping(value = "/process", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> processInput(@RequestBody ActionDto action) {
        Map<String, String> response = new HashMap<>();
        if (action.getActionType() == null || action.getActionType().isEmpty()) {
            response.put("error", "Action type is required");
            return ResponseEntity.badRequest().body(response);
        }
        TaskDto task = new TaskDto(action);
        if (Objects.nonNull(action.getSelectedBots()) && action.getSelectedBots().isEmpty()) {
            // send a broadcast message to all bots
            List<BotDto> bots = botService.getAllBots().collectList().block();
            if (bots == null || bots.isEmpty()) {
                response.put("error", "No bots available to process the action");
                return ResponseEntity.badRequest().body(response);
            }
            bots.stream().forEach(bot -> bot.getTasks().add(task));
            botService.updateBots(bots.stream().map(Bot::new).toList());
            // Send the command to frontend
            botCommanderSocketHandler.broadcastUpdate(bots);
            // Send the command to all bots
            kafkaProducer.sendMessage("all", task);
        } else
            action.getSelectedBots().stream().forEach(botId -> {
                // iterate over the selected bots and send the command to each bot
                // Save the command to the db
                BotDto result = botService.addNewTaskToBot(botId, task);
                // Send the command to the bot
                kafkaProducer.sendMessage(botId, task);
                // update socker handler with the bot updated
                botCommanderSocketHandler.broadcastUpdate(result);
            });
        response.put("status", "OK");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping(value = "/bot/{botId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> deleteBot(@PathVariable String botId) {
        Map<String, String> response = new HashMap<>();
        if (botId == null || botId.isEmpty()) {
            response.put("error", "Bot ID is required");
            return ResponseEntity.badRequest().body(response);
        }
        botService.deleteBot(botId);

        List<BotDto> bots = botService.getAllBots().collectList().block();
        if (Objects.nonNull(bots) && !bots.isEmpty()) {
            botService.updateBots(bots.stream().map(Bot::new).toList());
            botCommanderSocketHandler.broadcastUpdate(bots);
        }
        
        response.put("status", "OK");
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/payload/url", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> getPayloadUrl() {
        String payloadUrl = botService.getPayloadUrl();
        if (payloadUrl == null || payloadUrl.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Map<String, String> response = new HashMap<>();
        response.put("payloadUrl", payloadUrl);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/payload/download")
    public ResponseEntity<?> downloadPayload(@RequestBody Map<String, String> params) {
        return botService.getPayloadDownload(params);
    }

    
    @PostMapping(value = "/createprocess", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> createProcess(@RequestBody ProcessDto processDto) {
        Map<String, String> response = new HashMap<>();
        if (processDto.getActionType() == null || processDto.getActionType().isEmpty()) {
            response.put("error", "Action type is required");
            return ResponseEntity.badRequest().body(response);
        }

        if (processDto.getParameters() == null || processDto.getParameters().isEmpty()) {
            response.put("error", "Parameters is required");
            return ResponseEntity.badRequest().body(response);
        }
        processService.createProcess(processDto);
        response.put("status", "OK");
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/processes", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ProcessDto>> getProcesses() {
        return ResponseEntity.ok(processService.getAllProcesses().collectList().block());
    }

}