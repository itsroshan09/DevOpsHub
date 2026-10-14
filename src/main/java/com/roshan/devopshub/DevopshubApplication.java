package com.roshan.devopshub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class DevopshubApplication {

	public static void main(String[] args) {
		SpringApplication.run(DevopshubApplication.class, args);
	}

}
