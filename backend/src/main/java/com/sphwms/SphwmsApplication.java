package com.sphwms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SphwmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(SphwmsApplication.class, args);
    }
}
