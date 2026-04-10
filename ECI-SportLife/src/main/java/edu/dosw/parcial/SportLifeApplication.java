package edu.dosw.parcial;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "edu.dosw.parcial.persistence.repositories")
@EnableMongoRepositories(basePackages = "edu.dosw.parcial.persistence.mongo.repositories")
public class SportLifeApplication {
    public static void main(String[] args) {
        SpringApplication.run(SportLifeApplication.class, args);
    }
}
