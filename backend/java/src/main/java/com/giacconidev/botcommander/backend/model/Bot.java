package com.giacconidev.botcommander.backend.model;

import java.time.Instant;
import java.util.ArrayList;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.giacconidev.botcommander.backend.dto.BotDto;
import com.giacconidev.botcommander.backend.dto.TaskDto;
import com.giacconidev.botcommander.backend.utils.Utils;

import ch.qos.logback.core.util.StringUtil;
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
    private String geolocation;
    private String payloadType;
    private ArrayList<Task> tasks = new ArrayList<>();

    public Bot(BotDto botDto) {
        this.id = botDto.getId();
        this.name = botDto.getName() != null ? botDto.getName() : Utils.generateRandomName();
        this.lastSignal = Instant.ofEpochMilli(botDto.getLastSignal());
        this.os = botDto.getOs();
        this.tasks = new ArrayList<>();
        for (TaskDto taskDto : botDto.getTasks()) {
            this.tasks.add(new Task(taskDto));
        }
        this.geolocation = botDto.getGeolocation();
        this.payloadType = botDto.getPayloadType();
    }

    public void refreshData(BotDto botDto) {
        // refresh last signal
        this.lastSignal = Instant.now();
        // refresh os platform
        if (!StringUtil.isNullOrEmpty(botDto.getOs()))
            this.os = botDto.getOs();
        // refresh os name
        if (!StringUtil.isNullOrEmpty(botDto.getName()))
            this.name = botDto.getName();
        // refresh geolocation
        if (!StringUtil.isNullOrEmpty(botDto.getGeolocation()))
            this.geolocation = botDto.getGeolocation();
        // refresh payload
        if (!StringUtil.isNullOrEmpty(botDto.getPayloadType()))
            this.payloadType = botDto.getPayloadType();
    }
}
