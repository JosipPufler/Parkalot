package hr.algebra.parkalot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ParkalotApplication {

    public static void main(String[] args) {
        SpringApplication.run(ParkalotApplication.class, args);
    }

}
