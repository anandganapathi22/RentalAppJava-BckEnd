package com.rentalapps.exception;

import com.rentalapps.database.DatabaseException;
import com.rentalapps.database.ErrorRespObj;
import com.rentalapps.database.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Global exception handler that maps application exceptions to appropriate HTTP responses.
 * Handles ResourceNotFoundException, NoContentException, ServiceException, and DatabaseException.
 */
@ControllerAdvice
public class ApplicationExceptionHandler extends ResponseEntityExceptionHandler {
  
  Logger logger = LoggerFactory.getLogger(ApplicationExceptionHandler.class);
  
  /** Handles ResourceNotFoundException and returns a 404 response. */
  @ResponseStatus(
        value = HttpStatus.NOT_FOUND,
        reason = "Resource Not Found")
  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ApiError> handleException(ResourceNotFoundException e) {
    ApiError error = new ApiError(HttpStatus.NOT_FOUND, e.getLocalizedMessage(),
          "Please check the resource path is correct and try again");
    return new ResponseEntity<>(error, error.getStatus());
  }

  /** Handles NoContentException and returns a 204/404 response. */
  @ResponseStatus(
        value = HttpStatus.NO_CONTENT,
        reason = "No Content to display.Please check the resource path is correct "
              + "and the entries in the resource are comma(,) separated and try again.")
  @ExceptionHandler(NoContentException.class)
  public ResponseEntity<ApiError> handleException(NoContentException e) {
    ApiError error = new ApiError(HttpStatus.NOT_FOUND, e.getLocalizedMessage(),
          "No content to display");
    return new ResponseEntity<>(error, error.getStatus());
  }

  /** Handles ServiceException and returns a structured error response with 404 status. */
  @ExceptionHandler(ServiceException.class)
  @ResponseStatus(value = HttpStatus.NOT_FOUND)
  public @ResponseBody ErrorRespObj handleServiceException(final ServiceException ex) {
 
    logger.info("Start of com.rentalapps.exception.handleServiceException() ...");
    
    ErrorRespObj error = new ErrorRespObj();
    error.setErrorType(ex.getErrorType());
    error.setErrorMessage(ex.getErrorMessage());
    error.setErrorReason(ex.getErrorReason());
    error.setErrorCode(ex.getErrorCode());

    logger.info("End of com.rentalapps.exception.handleServiceException() ...");
    return error;
  }
  
  /** Handles DatabaseException and returns a structured error response with 400 status. */
  @ExceptionHandler(DatabaseException.class)
  @ResponseStatus(value = HttpStatus.BAD_REQUEST)
  public @ResponseBody ErrorRespObj handleDatabaseException(final DatabaseException ex) {
 
    logger.info("Start of com.rentalapps.exception.handleDatabaseException() ...");
    
    ErrorRespObj error = new ErrorRespObj();
    error.setErrorType(ex.getErrorType());
    error.setErrorMessage(ex.getErrorMessage());
    error.setErrorReason(ex.getErrorReason());
    error.setErrorCode(ex.getErrorCode());

    logger.info("End of com.rentalapps.exception.handleDatabaseException() ...");
    return error;
  }

}
