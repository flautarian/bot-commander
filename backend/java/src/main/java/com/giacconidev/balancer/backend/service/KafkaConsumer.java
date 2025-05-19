package com.giacconidev.balancer.backend.service;

import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Profile("!test") 
public class KafkaConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaConsumer.class);

    @KafkaListener(topics = "callback")
    public void listen(String message) {
        LOGGER.info("Received Message in group 'apps-consumed': " + message);
    }
}
