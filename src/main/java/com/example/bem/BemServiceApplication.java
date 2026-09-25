package com.example.bem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the blade element momentum aerodynamic calculation service.
 */
@SpringBootApplication
public class BemServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BemServiceApplication.class, args);
    }
}
