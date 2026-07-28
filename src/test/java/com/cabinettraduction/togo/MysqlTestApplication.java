package com.cabinettraduction.togo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

@Configuration
public class MysqlTestApplication {

	@ServiceConnection
	@Profile("mysql")
	@Bean
	static MySQLContainer container() {
		return new MySQLContainer(DockerImageName.parse("mysql:9.7"));
	}

	public static void main(String[] args) {
		SpringApplication.run(CabinetTraductionApplication.class, "--spring.profiles.active=mysql",
				"--spring.docker.compose.enabled=false");
	}

}
