package com.twotrack.notebook;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TwoTrackNotebookApplication {

	public static void main(String[] args) {
		SpringApplication.run(TwoTrackNotebookApplication.class, args);
	}

}
