package com.giacconidev.botcommander.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateTaskEventDto {

    @JsonProperty("botId")
    private String botId;

    @JsonProperty("taskId")
    private String taskId;

    @JsonProperty("result")
    private String result;
}
