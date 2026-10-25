package com.corpoviva.vivent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ViventApplication {

	public static void main(String[] args) {
		SpringApplication.run(ViventApplication.class, args);
	}

}
