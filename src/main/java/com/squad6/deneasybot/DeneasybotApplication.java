package com.squad6.deneasybot;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DeneasybotApplication {

    public static void main(String[] args) {

        Dotenv dotenv = Dotenv.configure()
                .directory(System.getProperty("user.dir"))
                .load();

        SpringApplication.run(DeneasybotApplication.class, args);
    }

}
