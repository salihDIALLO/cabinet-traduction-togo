package com.cabinettraduction.togo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportRuntimeHints;

/**
 * Cabinet de Traduction Certifiée et Interprétation - Togo
 * Application Spring Boot principale.
 */
@SpringBootApplication
@ImportRuntimeHints(CabinetTraductionRuntimeHints.class)
public class CabinetTraductionApplication {

	public static void main(String[] args) {
		SpringApplication.run(CabinetTraductionApplication.class, args);
	}

}
