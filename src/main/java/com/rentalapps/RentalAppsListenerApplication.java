package com.rentalapps;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Rental Apps Spring Boot microservice.
 * Enables JMS message listening and scheduled task execution.
 */
@SpringBootApplication
@EnableJms
@EnableScheduling
public class RentalAppsListenerApplication extends SpringBootServletInitializer {
  @Override
  protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
    return application.sources(RentalAppsListenerApplication.class);
  }

  /** Bootstraps the Spring Boot application. */
  public static void main(String[] args) {
    SpringApplication.run(RentalAppsListenerApplication.class, args);
  }
}
