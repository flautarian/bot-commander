package com.giacconidev.botcommander.backend.model;

import java.time.Instant;
import java.util.ArrayList;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.giacconidev.botcommander.backend.dto.BotDto;
import com.giacconidev.botcommander.backend.dto.TaskDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Data
@Document(collection = "bots")
public class Bot {

    @Id
    private String id;
    private String name;
    private Instant lastSignal = Instant.now();
    private String os;
    private ArrayList<Task> tasks = new ArrayList<>();

    public Bot(BotDto botDto) {
        this.id = botDto.getId();
        this.name = botDto.getName();
        this.lastSignal = Instant.ofEpochMilli(botDto.getLastSignal());
        this.os = botDto.getOs();
        this.tasks = new ArrayList<>();
        for (TaskDto taskDto : botDto.getTasks()) {
            this.tasks.add(new Task(taskDto));
        }
    }
}
