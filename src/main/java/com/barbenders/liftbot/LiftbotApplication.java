package com.barbenders.liftbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

@SpringBootApplication
@ServletComponentScan
public class LiftbotApplication {

	public static void main(String[] args) {
		SpringApplication.run(LiftbotApplication.class, args);
	}

}
