package com.era.backend.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declares the Kafka topics used by Era (see architecture doc section 8.1).
 * Spring Kafka will auto-create these on startup against the configured broker.
 */
@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic messageSentTopic() {
        return TopicBuilder.name("era.message.sent").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic aiCommandTopic() {
        return TopicBuilder.name("era.ai.command").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic presenceChangeTopic() {
        return TopicBuilder.name("era.presence.change").partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic reminderDueTopic() {
        return TopicBuilder.name("era.reminder.due").partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic analyticsTopic() {
        return TopicBuilder.name("era.analytics").partitions(1).replicas(1).build();
    }
}
