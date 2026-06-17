package com.era.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/**
 * MongoDB is the primary data store (users, conversations, messages, era
 * sessions). Instant is natively handled by Spring Data MongoDB's default
 * codecs, so no custom Converters are required out of the box - add them
 * here if a field type ever needs bespoke (de)serialization.
 */
@Configuration
@EnableMongoAuditing
public class MongoConfig {
}
