package com.drivex;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.mongodb.client.MongoClients;

@SpringBootApplication
@EnableScheduling
public class DriveXApplication {

    public static void main(String[] args) {
        SpringApplication.run(DriveXApplication.class, args);
    }

    @Bean
    public MongoTemplate mongoTemplate() throws Exception {
        return new MongoTemplate(
            MongoClients.create("mongodb://localhost:27017"),
            "drivex"
        );
    }
}