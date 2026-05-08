package com.rassini.pagos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.rassini.pagos.repository")
@EntityScan(basePackages = "com.rassini.pagos.entity")
public class PagosLoaderApplication {

	public static void main(String[] args) {
		SpringApplication.run(PagosLoaderApplication.class, args);
	}

}
