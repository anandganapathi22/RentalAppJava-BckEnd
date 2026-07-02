package com.rentalapps.exception;


import java.util.Collections;
import java.util.List;
import org.springframework.http.HttpStatus;


/**
 * Immutable API error response containing HTTP status, message, and error details.
 */
public class ApiError {

  private final HttpStatus status;
  private final String message;
  private final List<String> errors;

  public ApiError(HttpStatus status, String message, List<String> errors) {
    super();
    this.status = status;
    this.message = message;
    this.errors = errors;
  }

  public ApiError(HttpStatus status, String message, String error) {
    super();
    this.status = status;
    this.message = message;
    errors = Collections.singletonList(error);
  }

  public HttpStatus getStatus() {
    return status;
  }
}