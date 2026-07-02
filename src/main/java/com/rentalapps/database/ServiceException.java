package com.rentalapps.database;

/**
 * Exception thrown when a service-layer operation fails.
 * Carries error type, message, reason, and HTTP status code.
 */
public class ServiceException extends ParentException {

  private static final long serialVersionUID = 1L;

  public ServiceException(String s) {
    super(s);
  }

  public ServiceException(String errorType, String errorMessage,
                           String errorReason, String errorCode) {
    super(errorType, errorMessage, errorReason, errorCode);
  }
}
