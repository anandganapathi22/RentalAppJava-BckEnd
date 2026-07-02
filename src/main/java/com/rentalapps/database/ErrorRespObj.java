package com.rentalapps.database;

/**
 * Response object representing error details returned to API callers.
 */
public class ErrorRespObj {

  private String errorType;
  private String errorMessage;
  private String errorReason;
  private String errorCode;
  
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