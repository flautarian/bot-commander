package com.giacconidev.botcommander.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.giacconidev.botcommander.backend.model.Process;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProcessDto {

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("actionType")
    private String actionType;

    @JsonProperty("parameters")
    private String parameters;

    public ProcessDto(Process p) {
        this.name = p.getName();
        this.description = p.getDescription();
        this.actionType = p.getActionType();
        this.parameters = p.getParameters().getOrDefault("value", "");
    }

}
