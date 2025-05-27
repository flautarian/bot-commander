package com.giacconidev.botcommander.backend.dto;

import java.util.ArrayList;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ActionDto {

    @JsonProperty("selectedBots")
    private ArrayList<String> selectedBots = new ArrayList<>();

    @JsonProperty("actionType")
    private String actionType;

    @JsonProperty("parameters")
    private Map<String, String> parameters;
}
