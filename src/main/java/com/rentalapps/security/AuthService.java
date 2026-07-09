package com.rentalapps.security;

import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/** Authenticates users against the database-backed users table. */
@Service
public class AuthService {
  private final AuthUserRepository authUserRepository;
  private final PasswordEncoder passwordEncoder;

  public AuthService(AuthUserRepository authUserRepository, PasswordEncoder passwordEncoder) {
    this.authUserRepository = authUserRepository;
    this.passwordEncoder = passwordEncoder;
  }

  public Optional<AuthenticatedUser> authenticate(String username, String password) {
    if (StringUtils.isBlank(username) || password == null) {
      return Optional.empty();
    }

    return authUserRepository.findByUsername(username)
        .filter(AuthUser::isEnabled)
        .filter(user -> passwordEncoder.matches(password, user.getPasswordHash()))
        .map(user -> new AuthenticatedUser(user.getUsername(), user.getRole()));
  }
}
