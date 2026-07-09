package com.rentalapps.security;

/** Authenticated user data returned by the login service. */
public class AuthenticatedUser {
  private final String username;
  private final String role;

  public AuthenticatedUser(String username, String role) {
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
