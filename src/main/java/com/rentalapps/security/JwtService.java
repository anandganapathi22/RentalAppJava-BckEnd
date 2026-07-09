package com.rentalapps.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

/** Creates and validates local HS256 JWT tokens. */
@Service
public class JwtService {
  private static final String HMAC_SHA256 = "HmacSHA256";
  private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

  private final JwtAuthProperties properties;
  private final ObjectMapper objectMapper;

  public JwtService(JwtAuthProperties properties, ObjectMapper objectMapper) {
    this.properties = properties;
    this.objectMapper = objectMapper;
  }

  public String createToken(String subject, String role) {
    Instant now = Instant.now();
    long expiresAt = now.plusSeconds(properties.getTokenExpirationSeconds()).getEpochSecond();

    Map<String, Object> header = new LinkedHashMap<>();
    header.put("alg", "HS256");
    header.put("typ", "JWT");

    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("sub", subject);
    payload.put("role", normalizeRole(role));
    payload.put("iat", now.getEpochSecond());
    payload.put("exp", expiresAt);

    String unsignedToken = encodeJson(header) + "." + encodeJson(payload);
    return unsignedToken + "." + sign(unsignedToken);
  }

  public JwtPrincipal validateAndGetPrincipal(String token) {
    String[] parts = token.split("\\.");
    if (parts.length != 3) {
      throw new IllegalArgumentException("Invalid JWT format");
    }

    String unsignedToken = parts[0] + "." + parts[1];
    if (!constantTimeEquals(sign(unsignedToken), parts[2])) {
      throw new IllegalArgumentException("Invalid JWT signature");
    }

    Map<String, Object> payload = decodeJson(parts[1]);
    long expiresAt = getLongClaim(payload, "exp");
    if (Instant.now().getEpochSecond() >= expiresAt) {
      throw new IllegalArgumentException("JWT is expired");
    }

    Object subject = payload.get("sub");
    if (!(subject instanceof String username) || username.isBlank()) {
      throw new IllegalArgumentException("JWT subject is missing");
    }
    Object role = payload.get("role");
    if (!(role instanceof String roleName) || roleName.isBlank()) {
      throw new IllegalArgumentException("JWT role is missing");
    }
    return new JwtPrincipal(username, normalizeRole(roleName));
  }

  public long getTokenExpirationSeconds() {
    return properties.getTokenExpirationSeconds();
  }

  private String encodeJson(Map<String, Object> values) {
    try {
      return base64UrlEncode(objectMapper.writeValueAsBytes(values));
    } catch (Exception e) {
      throw new IllegalStateException("Could not encode JWT", e);
    }
  }

  private Map<String, Object> decodeJson(String encodedJson) {
    try {
      return objectMapper.readValue(Base64.getUrlDecoder().decode(encodedJson), MAP_TYPE);
    } catch (Exception e) {
      throw new IllegalArgumentException("Could not decode JWT", e);
    }
  }

  private long getLongClaim(Map<String, Object> payload, String name) {
    Object value = payload.get(name);
    if (value instanceof Number number) {
      return number.longValue();
    }
    throw new IllegalArgumentException("JWT claim is missing: " + name);
  }

  private String sign(String value) {
    try {
      Mac mac = Mac.getInstance(HMAC_SHA256);
      mac.init(new SecretKeySpec(properties.getJwtSecret().getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
      return base64UrlEncode(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException("Could not sign JWT", e);
    }
  }

  private String normalizeRole(String role) {
    String normalizedRole = role == null || role.isBlank() ? "USER" : role.trim().toUpperCase();
    return normalizedRole.startsWith("ROLE_") ? normalizedRole.substring("ROLE_".length()) : normalizedRole;
  }

  private String base64UrlEncode(byte[] value) {
    return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
  }

  private boolean constantTimeEquals(String left, String right) {
    byte[] leftBytes = left.getBytes(StandardCharsets.UTF_8);
    byte[] rightBytes = right.getBytes(StandardCharsets.UTF_8);
    if (leftBytes.length != rightBytes.length) {
      return false;
    }

    int result = 0;
    for (int i = 0; i < leftBytes.length; i++) {
      result |= leftBytes[i] ^ rightBytes[i];
    }
    return result == 0;
  }
}
