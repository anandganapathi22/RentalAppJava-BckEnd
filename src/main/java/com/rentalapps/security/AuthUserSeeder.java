package com.rentalapps.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Seeds the first local auth user when the configured username is not present. */
@Configuration
public class AuthUserSeeder {
  private static final Logger logger = LoggerFactory.getLogger(AuthUserSeeder.class);

  @Bean
  public ApplicationRunner seedAuthUser(AuthUserRepository authUserRepository,
                                        JwtAuthProperties properties,
                                        PasswordEncoder passwordEncoder) {
    return args -> {
      if (authUserRepository.findByUsername(properties.getUsername()).isPresent()) {
        return;
      }

      authUserRepository.createUser(
          properties.getUsername(),
          passwordEncoder.encode(properties.getPassword()),
          properties.getRole());
      logger.info("Seeded auth user username={} role={}", properties.getUsername(), properties.getRole());
    };
  }
}
