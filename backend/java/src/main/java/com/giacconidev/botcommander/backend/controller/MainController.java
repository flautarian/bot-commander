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
        if (params.get("payloadType") == null || params.get("payloadType").isEmpty()) {
            return ResponseEntity.badRequest().body("Payload type is required");
        }
        if (params.get("payloadUrl") == null || params.get("payloadUrl").isEmpty()) {
            return ResponseEntity.badRequest().body("Payload URL is required");
        }
        // Download the payload
        try {

            String configContent = "bootstrap_servers=" + params.get("payloadUrl") + "\n" +
                    "topic=apps\n";

            // Create a ZIP file in the temporary directory
            Path zipFilePath = Files.createTempFile("python-payload", ".zip");

            // Zip the Python project
            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFilePath.toFile()))) {
                ArrayList<String> bannedFiles = new ArrayList<>(Arrays.asList("config.txt", "myenv", "__pycache__"));
                // Copy the Python project files to the ZIP
                copyFolderToZip(params.get("payloadType") + "-project", bannedFiles, zos);

                // Add a file called config file to the ZIP
                ZipEntry configEntry = new ZipEntry("config.txt");
                // add configContent content to the config file
                zos.putNextEntry(configEntry);
                zos.write(configContent.getBytes());
                zos.closeEntry();
            } finally {
                // Clean up the temporary file on exit
                zipFilePath.toFile().deleteOnExit();
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

    public void copyFolderToZip(String folderPath, ArrayList<String> bannedFiles, ZipOutputStream fos)
            throws IOException {
        // Create a resolver to find resources matching a pattern
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        // Use the resolver to get resources from the specified folder
        Resource[] resources = resolver.getResources("classpath:" + folderPath + "/*");

        // Create a ZipOutputStream to write the ZIP file
        for (Resource resource : resources) {
            // Skip banned files
            if (bannedFiles.contains(resource.getFilename())) {
                continue;
            }
            // Create a new ZipEntry for each file
            String fileName = resource.getFilename();
            ZipEntry zipEntry = new ZipEntry(fileName);
            fos.putNextEntry(zipEntry);

            // Copy the content of the resource to the ZIP file
            InputStream is = resource.getInputStream();
            StreamUtils.copy(is, fos);
            is.close();

            fos.closeEntry();
        }
    }

}