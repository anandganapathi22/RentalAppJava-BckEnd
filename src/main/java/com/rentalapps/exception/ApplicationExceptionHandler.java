package com.rentalapps.exception;

import com.rentalapps.vo.ErrorRespObj;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Global exception handler that maps application exceptions to appropriate HTTP responses.
 * Handles ResourceNotFoundException, NoContentException, ServiceException, and DatabaseException.
 */
@ControllerAdvice
public class ApplicationExceptionHandler extends ResponseEntityExceptionHandler {
  
  Logger logger = LoggerFactory.getLogger(ApplicationExceptionHandler.class);
  
  /** Handles ResourceNotFoundException and returns a 404 response. */
  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ApiError> handleException(ResourceNotFoundException e) {
    ApiError error = new ApiError(HttpStatus.NOT_FOUND, e.getLocalizedMessage(),
          "Please check the resource path is correct and try again");
    return new ResponseEntity<>(error, error.getStatus());
  }

  /** Handles NoContentException and returns a 204 response. */
  @ExceptionHandler(NoContentException.class)
  public ResponseEntity<Void> handleException(NoContentException e) {
    return ResponseEntity.noContent().build();
  }

  /** Handles ServiceException and returns a structured error response with 404 status. */
  @ExceptionHandler(ServiceException.class)
  public ResponseEntity<ErrorRespObj> handleServiceException(final ServiceException ex) {
 
    logger.info("Start of com.rentalapps.exception.handleServiceException() ...");
    
    ErrorRespObj error = new ErrorRespObj();
    error.setErrorType(ex.getErrorType());
    error.setErrorMessage(ex.getErrorMessage());
    error.setErrorReason(ex.getErrorReason());
    error.setErrorCode(ex.getErrorCode());

    logger.info("End of com.rentalapps.exception.handleServiceException() ...");
    return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
  }
  
  /** Handles DatabaseException and returns a structured error response with 400 status. */
  @ExceptionHandler(DatabaseException.class)
  public ResponseEntity<ErrorRespObj> handleDatabaseException(final DatabaseException ex) {
 
    logger.info("Start of com.rentalapps.exception.handleDatabaseException() ...");
    
    ErrorRespObj error = new ErrorRespObj();
    error.setErrorType(ex.getErrorType());
    error.setErrorMessage(ex.getErrorMessage());
    error.setErrorReason(ex.getErrorReason());
    error.setErrorCode(ex.getErrorCode());

    logger.info("End of com.rentalapps.exception.handleDatabaseException() ...");
    return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
  }

}
