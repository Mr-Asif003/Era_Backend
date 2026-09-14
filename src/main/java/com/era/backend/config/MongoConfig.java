package com.era.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/**
 * MongoDB is the primary (and only) data store: users, conversations,
 * messages. Instant is natively handled by Spring Data MongoDB's default
 * codecs, so no custom Converters are required out of the box.
 */
@Configuration
@EnableMongoAuditing
public class MongoConfig {
}
