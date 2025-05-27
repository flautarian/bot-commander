package com.giacconidev.botcommander.backend.service;

import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giacconidev.botcommander.backend.dto.BotDto;
import com.giacconidev.botcommander.backend.dto.HeartBeatEventDto;
import com.giacconidev.botcommander.backend.dto.TaskDto;
import com.giacconidev.botcommander.backend.dto.UpdateTaskEventDto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Profile("!test") 
public class KafkaConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaConsumer.class);

    private BotService botService;

    private BotCommanderSocketHandler botCommanderSocketHandler;

    public KafkaConsumer(BotService botService, BotCommanderSocketHandler botCommanderSocketHandler) {
        this.botCommanderSocketHandler = botCommanderSocketHandler;
        this.botService = botService;
    }

    @KafkaListener(topics = "callback")
    public void listenCallbackMessages(String message) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            TaskDto task = objectMapper.readValue(message, TaskDto.class);
            // Process the task
            if(task.getActionType().equals("callback")) {
                // Handle the init action
                String taskId = task.getParameters().get("taskId");
                String botId = task.getParameters().get("groupId");
                String result = task.getResult();
                LOGGER.info("Update task received for bot: " + botId + " with taskId: " + taskId);
                // Update the task in the bot
                botService.updateTaskInBot(botId, taskId, result);
                // Notify all connected clients
                botCommanderSocketHandler.broadcastUpdate(new UpdateTaskEventDto(botId, taskId, result));
                LOGGER.info("Task updated for bot: " + botId + " with taskId: " + taskId);
            } else {
                LOGGER.warn("Unknown action type: " + task.getActionType());
            }
        } catch (JsonProcessingException e) {
            LOGGER.error("Error processing message: " + e.getMessage());
        }
        catch (Exception e) {
            LOGGER.error("Fatal error processing message: " + e.getMessage());
        }
    }

    @KafkaListener(topics = "heartbeat")
    public void listenHeartBeatMessages(String message) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            TaskDto task = objectMapper.readValue(message, TaskDto.class);
            // Process the task
            if(task.getActionType().equals("heartbeat")) {
                // Handle the init action
                String botId = task.getParameters().get("botId");
                LOGGER.info("Bot: " + botId + " given life signals");
                // Update bot
                BotDto result = botService.InitializeOrRefreshBot(botId, null);
                // Notify all connected clients
                botCommanderSocketHandler.broadcastUpdate(new HeartBeatEventDto(result.getId(), result.getLastSignal()));
            } else {
                LOGGER.warn("Unknown action type: " + task.getActionType());
            }
        } catch (JsonProcessingException e) {
            LOGGER.error("Error processing message: " + e.getMessage());
        }
        catch (Exception e) {
            LOGGER.error("Fatal error processing message: " + e.getMessage());
        }
    }

    @KafkaListener(topics = "init")
    public void listenInitMessages(String message) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            TaskDto task = objectMapper.readValue(message, TaskDto.class);
            // Process the task
            if(task.getActionType().equals("init")) {
                // Handle the init action
                LOGGER.info("Init action received for task: " + task.getId());
                String botId = task.getParameters().get("groupId");
                String os = task.getParameters().get("os");
                BotDto botResult = botService.InitializeOrRefreshBot(botId, os);
                // Notify all connected clients
                botCommanderSocketHandler.broadcastUpdate(botResult);
            } else {
                LOGGER.warn("Unknown action type: " + task.getActionType());
            }
        } catch (JsonProcessingException e) {
            LOGGER.error("Error processing message: " + e.getMessage());
        }
        catch (Exception e) {
            LOGGER.error("Fatal error processing message: " + e.getMessage());
        }
    }

}
