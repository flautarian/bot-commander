package com.giacconidev.botcommander.backend.controller;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.context.annotation.Profile;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;

import com.giacconidev.botcommander.backend.dto.ActionDto;
import com.giacconidev.botcommander.backend.dto.BotDto;
import com.giacconidev.botcommander.backend.dto.TaskDto;
import com.giacconidev.botcommander.backend.model.Bot;
import com.giacconidev.botcommander.backend.service.BotCommanderSocketHandler;
import com.giacconidev.botcommander.backend.service.BotService;
import com.giacconidev.botcommander.backend.service.KafkaProducer;
import com.giacconidev.botcommander.backend.utils.Utils;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@RestController
@Profile("!test")
@RequestMapping("/api/v1")
public class MainController {

    private KafkaProducer kafkaProducer;
    private BotService botService;
    private BotCommanderSocketHandler botCommanderSocketHandler;

    public MainController(KafkaProducer kafkaProducer, BotService botService,
            BotCommanderSocketHandler botCommanderSocketHandler) {
        this.kafkaProducer = kafkaProducer;
        this.botService = botService;
        this.botCommanderSocketHandler = botCommanderSocketHandler;
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

}