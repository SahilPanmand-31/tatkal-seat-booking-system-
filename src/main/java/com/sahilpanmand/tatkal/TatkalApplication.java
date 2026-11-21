package com.sahilpanmand.tatkal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

// UserDetailsServiceAutoConfiguration is excluded because login is done with our own JWT,
// so Spring's default in-memory user is not needed.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class TatkalApplication {

    public static void main(String[] args) {
        SpringApplication.run(TatkalApplication.class, args);
    }
}
