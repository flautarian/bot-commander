package com.giacconidev.botcommander.backend.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.giacconidev.botcommander.backend.model.Task;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskDto {

    @JsonProperty("id")
    private String id = UUID.randomUUID().toString();

    @JsonProperty("actionType")
    private String actionType;

    @JsonProperty("parameters")
    private Map<String, String> parameters;
    
    @JsonProperty("creationDate")
    private Instant creationDate = Instant.now();

    @JsonProperty("result")
    private String result;

    public TaskDto(ActionDto action) {
        this.actionType = action.getActionType();
        this.parameters = action.getParameters();
        this.result = "";
    }

    public TaskDto(Task task) {
        this.id = task.getId();
        this.actionType = task.getActionType();
        this.parameters = task.getParameters();
        this.result = task.getResult();
    }

    @SuppressWarnings("unchecked")
    public TaskDto(Map<String, Object> body) {
        this.id = String.valueOf(body.getOrDefault("id",""));
        this.actionType = String.valueOf(body.getOrDefault("actionType",""));
        this.parameters = (Map<String, String>) body.getOrDefault("parameters", Map.of());
        this.result = String.valueOf(body.getOrDefault("result",""));
    }
}
