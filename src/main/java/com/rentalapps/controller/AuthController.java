package com.rentalapps.controller;

import com.rentalapps.security.AuthService;
import com.rentalapps.security.AuthenticatedUser;
import com.rentalapps.security.JwtService;
import com.rentalapps.vo.LoginRequest;
import com.rentalapps.vo.LoginResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Local authentication endpoint for issuing JWT bearer tokens. */
@RestController
@RequestMapping("/auth")
public class AuthController {
  private final AuthService authService;
  private final JwtService jwtService;

  public AuthController(AuthService authService, JwtService jwtService) {
    this.authService = authService;
    this.jwtService = jwtService;
  }

  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
    if (request == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    return authService.authenticate(request.getUsername(), request.getPassword())
        .map(this::loginResponse)
        .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
  }

  private ResponseEntity<LoginResponse> loginResponse(AuthenticatedUser user) {
    String token = jwtService.createToken(user.getUsername(), user.getRole());
    return ResponseEntity.ok(new LoginResponse(
        token,
        "Bearer",
        jwtService.getTokenExpirationSeconds(),
        user.getUsername(),
        user.getRole()));
  }
}
