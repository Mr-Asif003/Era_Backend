package com.era.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EraApplication {

    public static void main(String[] args) {
        SpringApplication.run(EraApplication.class, args);
    }

}
