package edu.dosw.parcial.main;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "edu.dosw.parcial")
public class SportLifeApplication {
    public static void main(String[] args) {
        SpringApplication.run(SportLifeApplication.class, args);
    }
}
