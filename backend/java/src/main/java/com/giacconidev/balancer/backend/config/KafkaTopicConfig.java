
package com.giacconidev.balancer.backend.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.Map;

@Configuration
@Profile("!test") 
public class KafkaTopicConfig {

    @Value("${spring.kafka.producer.bootstrap-servers}")
    private String kafkaProducerBootstrapServer;

    @Bean
    public NewTopic topic() {
        String topicName = "apps";
        return TopicBuilder.name(topicName).build();
    }

    @Bean
    public KafkaAdmin kafkaAdmin() {
        return new KafkaAdmin(Map.of("bootstrap.servers", kafkaProducerBootstrapServer));
    }
}