package com.giacconidev.balancer.backend.dto;

import java.util.ArrayList;
import java.util.Optional;

import org.bson.Document;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.giacconidev.balancer.backend.model.Bot;
import com.giacconidev.balancer.backend.model.Task;

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

    @JsonProperty("status")
    private String status = "";

    @JsonProperty("tasks")
    private ArrayList<TaskDto> tasks = new ArrayList<>();

    public BotDto(Bot bot) {
        this.id = bot.getId();
        this.name = bot.getName();
        this.status = bot.getStatus();
        ArrayList<TaskDto> tasks = new ArrayList<>();
        for (Task task : Optional.ofNullable(bot.getTasks()).orElse(new ArrayList<>())) {
            tasks.add(new TaskDto(task));
        }
        this.tasks = tasks;
    }

    @SuppressWarnings("unchecked")
    public BotDto(Document body) {
        this.id = String.valueOf(body.getOrDefault("id",""));
        this.name = String.valueOf(body.getOrDefault("name",""));
        this.status = String.valueOf(body.getOrDefault("status",""));
        this.tasks = new ArrayList<>();
        ArrayList<Document> tasks = (ArrayList<Document>) body.getOrDefault("tasks", new ArrayList<>());
        for (Document task : Optional.ofNullable(tasks).orElse(new ArrayList<>())) {
            this.tasks.add(new TaskDto(task));
        }
    }
}
