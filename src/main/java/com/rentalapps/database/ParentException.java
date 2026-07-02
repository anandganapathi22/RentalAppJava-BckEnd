package com.rentalapps.database;

/**
 * Base exception class for the application, carrying structured error details
 * (type, message, reason, and HTTP code) for consistent error handling.
 */
public class ParentException extends Exception {

  private static final long serialVersionUID = 1L;
  protected String errorType;
  private String errorMessage;
  private String errorReason;
  private String errorCode;

  public ParentException(String errorType, String errorMessage,
                         String errorReason, String errorCode) {
    this.errorType = errorType;
    this.errorMessage = errorMessage;
    this.errorReason = errorReason;
    this.errorCode = errorCode;
  }

  public ParentException(String s) {
    super(s);
  }

  public String getErrorType() {
    return errorType;
  }

  public void setErrorType(String errorType) {
    this.errorType = errorType;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public String getErrorReason() {
    return errorReason;
  }

  public void setErrorReason(String errorReason) {
    this.errorReason = errorReason;
  }

  public String getErrorCode() {
    return errorCode;
  }

  public void setErrorCode(String errorCode) {
    this.errorCode = errorCode;
  }
}
