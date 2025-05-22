package com.giacconidev.balancer.backend.repository;

import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import com.giacconidev.balancer.backend.model.Bot;
import reactor.core.publisher.Flux;

public interface BotRepository extends ReactiveMongoRepository<Bot, String> {
    @Query("{status : ?0}")
    Flux<Bot> findByStatus(String status);
}