package com.example.board_test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class BoardTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(BoardTestApplication.class, args);
    }

}
