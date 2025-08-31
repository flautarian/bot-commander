package com.giacconidev.botcommander.backend.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;

import org.bson.Document;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.giacconidev.botcommander.backend.model.Bot;
import com.giacconidev.botcommander.backend.model.Task;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BotDto {

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name = "";

    @JsonProperty("lastSignal")
    private long lastSignal = -1L;

    @JsonProperty("os")
    private String os = "";

    @JsonProperty("geolocation")
    private String geolocation = "";

    @JsonProperty("payloadType")
    private String payloadType = "";

    @JsonProperty("tasks")
    private ArrayList<TaskDto> tasks = new ArrayList<>();

    public BotDto(Bot bot) {
        this.id = bot.getId();
        this.name = bot.getName();
        this.lastSignal = bot.getLastSignal().toEpochMilli();
        this.os = bot.getOs();
        ArrayList<TaskDto> tasks = new ArrayList<>();
        for (Task task : Optional.ofNullable(bot.getTasks()).orElse(new ArrayList<>())) {
            tasks.add(new TaskDto(task));
        }
        this.tasks = tasks;
        this.geolocation = bot.getGeolocation();
        this.payloadType = bot.getPayloadType();
    }

    @SuppressWarnings("unchecked")
    public BotDto(Document body) {
        this.id = String.valueOf(body.getOrDefault("id",""));
        this.name = String.valueOf(body.getOrDefault("name",""));
        this.lastSignal = (Long) body.getOrDefault("lastSignal", Instant.now().toEpochMilli());
        this.tasks = new ArrayList<>();
        this.geolocation = String.valueOf(body.getOrDefault("geolocation",""));
        this.payloadType = String.valueOf(body.getOrDefault("payloadType",""));
        ArrayList<Document> tasks = (ArrayList<Document>) body.getOrDefault("tasks", new ArrayList<>());
        for (Document task : Optional.ofNullable(tasks).orElse(new ArrayList<>())) {
            this.tasks.add(new TaskDto(task));
        }
    }

    public BotDto(String id, String name, String os, String geolocation, String payloadType) {
        this.id = id;
        this.name = name;
        this.os = os;
        this.geolocation = geolocation;
        this.payloadType = payloadType;
    }

    public BotDto(String botId) {
        this.id = botId;
    }



    
}
