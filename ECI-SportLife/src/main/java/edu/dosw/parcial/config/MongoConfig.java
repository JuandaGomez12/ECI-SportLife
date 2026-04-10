package edu.dosw.parcial.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = "edu.dosw.parcial.persistence.mongo.repositories")
public class MongoConfig {
}
