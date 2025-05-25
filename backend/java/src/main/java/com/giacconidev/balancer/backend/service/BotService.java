package com.giacconidev.balancer.backend.service;

import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import com.giacconidev.balancer.backend.dto.BotDto;
import com.giacconidev.balancer.backend.dto.TaskDto;
import com.giacconidev.balancer.backend.model.Bot;
import com.giacconidev.balancer.backend.model.Task;
import com.giacconidev.balancer.backend.repository.BotRepository;
import com.giacconidev.balancer.backend.utils.Utils;

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

    public Flux<BotDto> getAllActiveBots() {
        return botRepository.findByStatus(Utils.BOT_ACTIVE).map(BotDto::new)
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
        return null; // or throw an exception if bot not found
    }

    public BotDto InitializeOrRefreshBot(String botId, String os) {
        Bot bot = botRepository.findById(botId).block();
        if (Objects.nonNull(bot)) {
            // refresh bot
            bot.setStatus(Utils.BOT_ACTIVE);
            // refresh os platform
            bot.setOs(os);
            // save bot
            botRepository.save(bot).block();
        }
        else {
            // create new bot
            bot = new Bot();
            bot.setId(botId);
            bot.setName("New Bot");
            bot.setOs(os);
            bot.setStatus(Utils.BOT_ACTIVE);
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
            // refresh bot
            bot.setStatus(Utils.BOT_ACTIVE);
            // save bot
            botRepository.save(bot).block();
        }
    }

    public String getPayloadUrl() {
        return kafkaConsumerBootstrapServer;
    }

}
