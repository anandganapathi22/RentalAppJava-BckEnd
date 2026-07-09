package com.rentalapps.security;

/** Principal claims extracted from a validated JWT. */
public class JwtPrincipal {
  private final String username;
  private final String role;

  public JwtPrincipal(String username, String role) {
    this.username = username;
    this.role = role;
  }

  public String getUsername() {
    return username;
  }

  public String getRole() {
    return role;
  }
}
