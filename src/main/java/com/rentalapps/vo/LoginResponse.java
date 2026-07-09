package com.rentalapps.vo;

/** JWT login response. */
public class LoginResponse {
  private String accessToken;
  private String tokenType;
  private long expiresInSeconds;
  private String username;
  private String role;

  public LoginResponse(String accessToken, String tokenType, long expiresInSeconds) {
    this(accessToken, tokenType, expiresInSeconds, null, null);
  }

  public LoginResponse(String accessToken, String tokenType, long expiresInSeconds, String username, String role) {
    this.accessToken = accessToken;
    this.tokenType = tokenType;
    this.expiresInSeconds = expiresInSeconds;
    this.username = username;
    this.role = role;
  }

  public String getAccessToken() {
    return accessToken;
  }

  public void setAccessToken(String accessToken) {
    this.accessToken = accessToken;
  }

  public String getTokenType() {
    return tokenType;
  }

  public void setTokenType(String tokenType) {
    this.tokenType = tokenType;
  }

  public long getExpiresInSeconds() {
    return expiresInSeconds;
  }

  public void setExpiresInSeconds(long expiresInSeconds) {
    this.expiresInSeconds = expiresInSeconds;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }
}
