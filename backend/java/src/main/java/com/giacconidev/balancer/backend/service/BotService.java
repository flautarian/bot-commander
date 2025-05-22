package com.giacconidev.balancer.backend.service;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.giacconidev.balancer.backend.dto.BotDto;
import com.giacconidev.balancer.backend.dto.TaskDto;
import com.giacconidev.balancer.backend.model.Bot;
import com.giacconidev.balancer.backend.model.Task;
import com.giacconidev.balancer.backend.repository.BotRepository;
import com.giacconidev.balancer.backend.utils.Utils;

import reactor.core.publisher.Flux;

@Service
public class BotService {

    private BotRepository botRepository;

    public BotService(BotRepository botRepository) {
        this.botRepository = botRepository;
    }
    
    private static final Logger logger = LoggerFactory.getLogger(BotService.class);

    public Flux<BotDto> getAllActiveBots() {
        return botRepository.findByStatus(Utils.BOT_ACTIVE).map(BotDto::new)
                .doOnError(error -> logger.error("Failed to get bots from db: " + error.getMessage()));
    }

    public void addNewTaskToBot(String botId, TaskDto input) {
        // find bot by id
        Bot bot = botRepository.findById(botId).block();
        if (Objects.nonNull(bot)) {
            // add new task to bot
            Task task = new Task(input.getId(), input.getActionType(), input.getParameters(), input.getResult());
            bot.getTasks().add(task);
            // save bot
            botRepository.save(bot).block();
        }
    }

    public void InitializeOrRefreshBot(String botId) {
        Bot bot = botRepository.findById(botId).block();
        if (Objects.nonNull(bot)) {
            // refresh bot
            bot.setStatus(Utils.BOT_ACTIVE);
            // save bot
            botRepository.save(bot).block();
        }
        else {
            // create new bot
            Bot newBot = new Bot();
            newBot.setId(botId);
            newBot.setName("New Bot");
            newBot.setStatus(Utils.BOT_ACTIVE);
            // save bot
            botRepository.save(newBot).block(); 
        }
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

}
