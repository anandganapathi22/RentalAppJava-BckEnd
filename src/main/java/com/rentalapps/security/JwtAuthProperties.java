package com.rentalapps.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Local username/password and JWT settings. */
@Component
@ConfigurationProperties(prefix = "rental.auth")
public class JwtAuthProperties {
  private String username = "admin";
  private String password = "admin";
  private String role = "ADMIN";
  private String jwtSecret = "local-development-jwt-secret-change-me";
  private long tokenExpirationSeconds = 3600;

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public String getJwtSecret() {
    return jwtSecret;
  }

  public void setJwtSecret(String jwtSecret) {
    this.jwtSecret = jwtSecret;
  }

  public long getTokenExpirationSeconds() {
    return tokenExpirationSeconds;
  }

  public void setTokenExpirationSeconds(long tokenExpirationSeconds) {
    this.tokenExpirationSeconds = tokenExpirationSeconds;
  }
}
