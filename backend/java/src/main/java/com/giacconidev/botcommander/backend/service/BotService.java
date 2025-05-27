package com.giacconidev.botcommander.backend.service;

import java.time.Instant;
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

    public Flux<BotDto> getAllBots() {
        return botRepository.findAll().map(BotDto::new)
                .doOnError(error -> logger.error("Failed to get bots from db: " + error.getMessage()));
    }

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

    public BotDto InitializeOrRefreshBot(String botId, String os) {
        Bot bot = botRepository.findById(botId).block();
        if (Objects.nonNull(bot)) {
            // refresh last signal
            bot.setLastSignal(Instant.now());
            // refresh os platform
            if (Objects.nonNull(os))
                bot.setOs(os);
            // save bot
            botRepository.save(bot).block();
        } else {
            // create new bot
            bot = new Bot();
            bot.setId(botId);
            bot.setName("New Bot");
            bot.setOs(os);
            // save bot
            botRepository.save(bot).block();
        }
        return new BotDto(bot);
    }

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

    public String getPayloadUrl() {
        return kafkaConsumerBootstrapServer;
    }

}
