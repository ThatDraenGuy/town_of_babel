package ru.itmo.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BabelApplication {

	public static void main(String[] args)
    {
		SpringApplication.run(BabelApplication.class, args);
	}

}
