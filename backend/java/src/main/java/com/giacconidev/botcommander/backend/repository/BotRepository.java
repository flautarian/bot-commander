package com.giacconidev.botcommander.backend.repository;

import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

import com.giacconidev.botcommander.backend.model.Bot;

import reactor.core.publisher.Flux;

public interface BotRepository extends ReactiveMongoRepository<Bot, String> {

    @Query("{ 'os' : ?0 }")
    Flux<Bot> findByOs(String os);

    @Query("{ 'name' : ?0 }")
    Flux<Bot> findByName(String name);

    @Query("{ 'lastSignal' : { $gt: ?0 } }")
    Flux<Bot> findByLastSignalGreaterThan(long lastSignal);
}