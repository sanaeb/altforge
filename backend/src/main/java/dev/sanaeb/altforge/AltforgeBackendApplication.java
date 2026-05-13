package dev.sanaeb.altforge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AltforgeBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(AltforgeBackendApplication.class, args);
	}

}
