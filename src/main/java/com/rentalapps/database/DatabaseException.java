package com.rentalapps.database;

/**
 * Exception thrown when a database operation fails.
 * Carries error type, message, reason, and HTTP status code.
 */
public class DatabaseException extends ParentException {

  private static final long serialVersionUID = 1L;

  public DatabaseException(String s) {
    super(s);
  }

  public DatabaseException(String errorType, String errorMessage,
                           String errorReason, String errorCode) {
    super(errorType, errorMessage, errorReason, errorCode);
  }
}
