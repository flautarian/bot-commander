
package com.giacconidev.balancer.backend.service;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.giacconidev.balancer.backend.dto.TaskDto;

@Service
@Profile("!test")
public class KafkaProducer {

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaProducer.class);

    private NewTopic topic;

    private KafkaTemplate<String, TaskDto> kafkaTemplate;

    public KafkaProducer(NewTopic topic, KafkaTemplate<String, TaskDto> kafkaTemplate) {
        this.topic = topic;
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendMessage(String botId, TaskDto event) {
        LOGGER.info(String.format("Send order => %s", event.toString()));

        // create order message
        Message<TaskDto> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, topic.name())
                .setHeader("recipientId", botId)
                .build();
        kafkaTemplate.send(message);
    }
}