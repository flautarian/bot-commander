package com.giacconidev.botcommander.backend.repository;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import com.giacconidev.botcommander.backend.model.Process;

public interface ProcessRepository extends ReactiveMongoRepository<Process, String> {
}