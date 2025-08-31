package com.giacconidev.botcommander.backend.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.giacconidev.botcommander.backend.dto.BotDto;
import com.giacconidev.botcommander.backend.dto.ProcessDto;
import com.giacconidev.botcommander.backend.repository.ProcessRepository;

import reactor.core.publisher.Flux;

import com.giacconidev.botcommander.backend.model.Process;

@Service
@Profile("!test")
public class ProcessService {

    private ProcessRepository processRepository;


    public ProcessService(ProcessRepository processRepository) {
        this.processRepository = processRepository;
    }

    private static final Logger logger = LoggerFactory.getLogger(ProcessService.class);

    public void createProcess(ProcessDto processDto) {
        Map<String, String> params = new HashMap<>();
        params.put("value", processDto.getParameters());
        Process process = new Process(null, processDto.getName(), processDto.getDescription(), processDto.getActionType(), params);
        processRepository.insert(process).block();
    }

    public Flux<ProcessDto> getAllProcesses() {
        return processRepository.findAll().map(ProcessDto::new)
                .doOnError(error -> logger.error("Failed to get processes from db: " + error.getMessage()));
    }
}