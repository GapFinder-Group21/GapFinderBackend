package com.backend.gapfinder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GapfinderApplication {

	public static void main(String[] args) {
		SpringApplication.run(GapfinderApplication.class, args);
	}

}
