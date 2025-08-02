package com.giacconidev.botcommander.backend.service;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
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
    @Value("${spring.kafka.consumer.bootstrap-servers}")
    private String kafkaConsumerBootstrapServer;

    public BotService(BotRepository botRepository) {
        this.botRepository = botRepository;
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
     * @return a BotDto representing the updated bot, or null if the bot does not exist
     */
    public BotDto addNewTaskToBot(String botId, TaskDto input) {
        // find bot by id
        Bot bot = botRepository.findById(botId).block();
        if (Objects.nonNull(bot)) {
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
     * @param os the operating system of the bot
     * @param name the name of the bot
     * @return a BotDto representing the initialized or refreshed bot
     */
    public BotDto InitializeOrRefreshBot(String botId, String os, String name) {
        Bot bot = botRepository.findById(botId).block();
        if (Objects.nonNull(bot)) {
            // refresh last signal
            bot.setLastSignal(Instant.now());
            // refresh os platform
            if (Objects.nonNull(os))
                bot.setOs(os);
            // refresh os name
            if (Objects.nonNull(name))
                bot.setName(name);
            // save bot
            botRepository.save(bot).block();
        } else {
            // create new bot
            bot = new Bot();
            bot.setId(botId);
            bot.setName(name != null ? name : Utils.generateRandomName());
            bot.setOs(os != null ? os : "Unknown");
            bot.setLastSignal(Instant.now());
            // save bot
            botRepository.save(bot).block();
        }
        return new BotDto(bot);
    }

    /**
     * Updates the task in the bot with the given botId and taskId.
     * 
     * @param botId the ID of the bot
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
        return kafkaConsumerBootstrapServer;
    }

    /**
     * Updates the list of bots in the database.
     * 
     * @param bots the list of bots to update
     */
    public void updateBots(List<Bot> bots) {
        botRepository.saveAll(bots);
    }

}
