package com.giacconidev.botcommander.backend.model;

import java.util.HashMap;
import java.util.Map;

import com.giacconidev.botcommander.backend.dto.TaskDto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Task {
    private String id;
    private String actionType;
    private Map<String, String> parameters = new HashMap<String,String>();
    private String result;

    public Task(TaskDto taskDto) {
        this.id = taskDto.getId();
        this.actionType = taskDto.getActionType();
        this.parameters = taskDto.getParameters();
        this.result = taskDto.getResult();
    }
}
