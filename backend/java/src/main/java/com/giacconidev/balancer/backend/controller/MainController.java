package com.giacconidev.balancer.backend.controller;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.context.annotation.Profile;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.giacconidev.balancer.backend.dto.ActionDto;
import com.giacconidev.balancer.backend.dto.BotDto;
import com.giacconidev.balancer.backend.dto.TaskDto;
import com.giacconidev.balancer.backend.service.BotService;
import com.giacconidev.balancer.backend.service.KafkaProducer;
import com.giacconidev.balancer.backend.service.BotCommanderSocketHandler;

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
        if (action.getActionType() == null || action.getActionType().isEmpty()){
            response.put("error", "Action type is required");
            return ResponseEntity.badRequest().body(response);
        }
        // iterate over the selected bots and send the command to each bot
        action.getSelectedBots().stream().forEach(botId -> {
            TaskDto task = new TaskDto(action);
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
        if (params.get("payloadType") == null || params.get("payloadType").isEmpty()) {
            return ResponseEntity.badRequest().body("Payload type is required");
        }
        if (params.get("payloadUrl") == null || params.get("payloadUrl").isEmpty()) {
            return ResponseEntity.badRequest().body("Payload URL is required");
        }
        // Download the payload
        try {
            // Path to the Python project in resources
            Path pythonProjectPath = Paths.get("src/main/resources/python-project");

            // Modify the configuration file
            Path configFilePath = pythonProjectPath.resolve("config.txt");
            Files.write(configFilePath, ("bootstrap_servers=" + params.get("payloadUrl") + "\n").getBytes());

            // Create a temporary ZIP file
            Path zipFilePath = Files.createTempFile("python-payload", ".zip");

            // Zip the Python project
            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFilePath.toFile()))) {
                Files.walk(pythonProjectPath)
                        .filter(path -> !Files.isDirectory(path))
                        .forEach(path -> {
                            ZipEntry zipEntry = new ZipEntry(pythonProjectPath.relativize(path).toString());
                            try {
                                zos.putNextEntry(zipEntry);
                                Files.copy(path, zos);
                                zos.closeEntry();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
            }

            // Serve the ZIP file as a downloadable response
            Resource resource = new UrlResource(zipFilePath.toUri());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (Exception e) {
            throw new RuntimeException("Error: " + e.getMessage());
        }
    }

}