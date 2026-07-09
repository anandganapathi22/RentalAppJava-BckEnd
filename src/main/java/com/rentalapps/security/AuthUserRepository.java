package com.rentalapps.security;

import com.rentalapps.config.ApplicationConfig;
import com.rentalapps.util.RentalDateTimeUtils;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** JDBC repository for application users. */
@Repository
public class AuthUserRepository {
  private final ApplicationConfig applicationConfig;
  private final JdbcTemplate jdbcTemplate;

  private final RowMapper<AuthUser> userMapper = (rs, rowNum) -> {
    AuthUser user = new AuthUser();
    user.setUsername(rs.getString("username"));
    user.setPasswordHash(rs.getString("passwordHash"));
    user.setRole(rs.getString("role"));
    user.setEnabled(rs.getBoolean("enabled"));
    return user;
  };

  public AuthUserRepository(ApplicationConfig applicationConfig, JdbcTemplate jdbcTemplate) {
    this.applicationConfig = applicationConfig;
    this.jdbcTemplate = jdbcTemplate;
  }

  public Optional<AuthUser> findByUsername(String username) {
    try {
      AuthUser user = jdbcTemplate.queryForObject(
          "select \"username\", \"passwordHash\", \"role\", \"enabled\" from " + usersTable()
              + " where \"username\" = ?",
          userMapper,
          normalizeUsername(username));
      return Optional.ofNullable(user);
    } catch (EmptyResultDataAccessException ex) {
      return Optional.empty();
    }
  }

  public void createUser(String username, String passwordHash, String role) {
    String now = RentalDateTimeUtils.getCurrentUtcTime();
    jdbcTemplate.update(
        "insert into " + usersTable()
            + " (\"username\", \"passwordHash\", \"role\", \"enabled\", \"createdDatetime\", \"updatedDatetime\") "
            + "values (?, ?, ?, ?, ?, ?)",
        normalizeUsername(username),
        passwordHash,
        normalizeRole(role),
        true,
        now,
        now);
  }

  private String usersTable() {
    return quote(applicationConfig.getUsersTable());
  }

  private String quote(String identifier) {
    return "\"" + StringUtils.defaultString(identifier).replace("\"", "\"\"") + "\"";
  }

  private String normalizeUsername(String username) {
    return StringUtils.trimToEmpty(username).toLowerCase();
  }

  private String normalizeRole(String role) {
    return StringUtils.defaultIfBlank(StringUtils.trimToEmpty(role), "USER").toUpperCase();
  }
}
