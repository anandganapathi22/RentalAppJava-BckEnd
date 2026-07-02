package com.rentalapps.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Component;

/**
 * Security configuration that permits all HTTP requests and disables CSRF.
 * Loaded with highest precedence to override default Spring Security behavior.
 */
@ComponentScan(basePackages = "com.rentalapps")
@Configuration
@Component("disableSecurityConfigurationBean")
@Order(value = Ordered.HIGHEST_PRECEDENCE)
public class DisableSecurityConfiguration {

  /** Configures the security filter chain to permit all requests and disable CSRF. */
  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests((authz) -> authz.anyRequest().permitAll())
            .csrf(AbstractHttpConfigurer::disable)
            .headers((headers) -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin));
    return http.build();
  }

}
