package com.neurogine.wallet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // Enable scheduled tasks for recurring payments
public class NeuroWalletApplication {

	public static void main(String[] args) {
		SpringApplication.run(NeuroWalletApplication.class, args);
	}

}
