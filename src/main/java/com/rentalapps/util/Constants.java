package com.rentalapps.util;

import org.springframework.stereotype.Component;

/**
 * Utility constants for file operations (JSON extension and path separator).
 */
@Component
public class Constants {
  public static final String EXTENSION = ".json";
  public static final String SLASH_SEPARATER = "/";

  private Constants() {
  }
}
