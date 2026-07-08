package com.rentalapps.util;

import com.rentalapps.exception.ServiceException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Locale;
import java.util.TimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class providing date/time formatting, timezone operations,
 * source system identification, and validation helpers.
 */
public class RentalDateTimeUtils {

  static Logger logger = LoggerFactory.getLogger(RentalDateTimeUtils.class);

  /** Returns the current UTC time as an ISO-8601 string. */
  public static String getCurrentUtcTime() {

    return Instant.now().toString();
  }

  /** Checks whether the given stall value matches a known zone name (e.g., PRES CIR, GOLD). */
  public static boolean isZone(String inputStall) {
    if (inputStall == null || inputStall.trim().isEmpty()) {
      return false;
    }
    String s = inputStall.trim().toUpperCase();
    return Arrays.stream(DatabaseConstants.ZONES).anyMatch(s::equals);
  }


  /** Returns the formatted local date-time string for a given continent and city. */
  public static String getCurrentLocalTime(String continent, String city)
        throws ServiceException {
    
    String  output = "";
    try {
      ZoneId zoneId = ZoneId.of(continent + "/" + city);
      ZonedDateTime now = ZonedDateTime.now(zoneId);
      output = DateTimeFormatter.ofPattern("dd-MMMM-yyyy, hh:mm a", Locale.ENGLISH).format(now);
    } catch (Exception ex) {
      logger.error("Exception -----> " + ex.getMessage());
      ex.printStackTrace();
      throw new ServiceException(DatabaseConstants.ERROR_TYPE_APPLICATION,
            DatabaseConstants.LOCALTIME_ERROR_MESSAGE,
            ex.getMessage(), DatabaseConstants.HTTP_CODE_500);
    }
    
    return output;
  }

  /** Returns the current local date-time as a LocalDateTime for the given timezone ID. */
  public static LocalDateTime getCurrentLocalTime(String timeZone) 
      throws ServiceException {
    
    LocalDateTime  output = null;
    try {
      ZoneId zoneId = ZoneId.of(timeZone);
      ZonedDateTime now = ZonedDateTime.now(zoneId);
      output = now.toLocalDateTime();
    } catch (Exception ex) {
      logger.error("Exception -----> " + ex.getMessage());
      ex.printStackTrace();
      throw new ServiceException(DatabaseConstants.ERROR_TYPE_APPLICATION,
            DatabaseConstants.LOCALTIME_ERROR_MESSAGE,
            ex.getMessage(), DatabaseConstants.HTTP_CODE_500);
    }
    return output;
  }

  /** Returns the short display name of the timezone for a given continent and city. */
  public static String getTimeZone(String continent, String city) 
      throws ServiceException {
    
    String output = "";
    try {
      output = TimeZone.getTimeZone(continent + "/" + city).getDisplayName(false, TimeZone.SHORT);
    } catch (Exception ex) {
      logger.error("Exception -----> " + ex.getMessage());
      ex.printStackTrace();
      throw new ServiceException(DatabaseConstants.ERROR_TYPE_APPLICATION,
            DatabaseConstants.LOCALTIME_ERROR_MESSAGE,
            ex.getMessage(), DatabaseConstants.HTTP_CODE_500);
    }
    return output;
  }
  
  /** Parses arrival date (MM/dd/yyyy) and time (HH:mm) strings into a LocalDateTime. */
  public static LocalDateTime formatArrivalDateTime(String arrivalDate, String arrivalTime) 
      throws ServiceException {
    
    LocalDateTime  output = null;
    try {
      LocalDate arrivalDatePart = null;   
      arrivalDatePart = LocalDate.parse(DatabaseConstants.DATE_FORMATTER_TARGET.format(
        DatabaseConstants.DATE_FORMATTER_SOURCE.parse(arrivalDate)));
      output = LocalDateTime.of(arrivalDatePart, LocalTime.parse(arrivalTime, DateTimeFormatter.ofPattern("HH:mm")));
    } catch (Exception ex) {
      logger.error("Exception from com.rentalapps.util.RentalDateTimeUtils -----> " + ex.getMessage());
      ex.printStackTrace();
      throw new ServiceException(DatabaseConstants.ERROR_TYPE_APPLICATION,
            DatabaseConstants.LOCALTIME_ERROR_MESSAGE,
            ex.getMessage(), DatabaseConstants.HTTP_CODE_500);
    }
    return output;
  }

  /** Validates a date string against MM/dd/yyyy format. Returns empty string if valid, error message otherwise. */
  public static String isValidDate(String inputDate) {

    String output = "";
    try {
      LocalDate.parse(inputDate, DatabaseConstants.DATE_FORMATTER_SOURCE);
    } catch (DateTimeParseException ex) {
      
      output = ex.getMessage();
    }
    return output;
  }
  
  /** Validates a time string against HH:mm format. Returns empty string if valid, error message otherwise. */
  public static String isValidTime(String inputTime) {
    
    String output = "";
    try {
      LocalTime.parse(inputTime, DatabaseConstants.TIME_FORMATTER_SOURCE);
    } catch (DateTimeParseException ex) {
      
      output = ex.getMessage();
    }
    return output;
  }

  /** Ensures month and day components of a date string are zero-padded to two digits. */
  public static String getTwoDigitDayMonth(String dateString) {
    if (dateString == null) {
      throw new IllegalArgumentException("Arrival date is required in MM/dd/yyyy format");
    }
    String[] str = dateString.split("/");
    if (str.length != 3) {
      throw new IllegalArgumentException("Arrival date must be in MM/dd/yyyy format: " + dateString);
    }
    int month = Integer.parseInt(str[0]);
    int day = Integer.parseInt(str[1]);
    int year = Integer.parseInt(str[2]);
    String output = String.format("%02d/%02d/%d", month, day, year);
    logger.info("Input date:" + dateString + ", Formatted date:" + output);
    return output;
  }

  /** Determines the source system (CWA, DASH, COUNTER-DESK) based on the RA value pattern. */
  public static String getSourceSystem(String ra, boolean qualTesting) {
    if (ra == null || ra.isEmpty()) {
      return "UNKNOWN"; // deterministic fallback
    }

    // Special exact match: RA12345
    if (DatabaseConstants.RENTALAPPS_SOURCE_UI_DEFAULT_VAL.equals(ra)) {
      return DatabaseConstants.RENTALAPPS_SOURCE_UI_SYSTEM;  // GB-UI
    }

    // Numeric RA
    if (ra.matches("\\d+")) {
      return DatabaseConstants.RENTALAPPS_SOURCE_DASH_SYSTEM;
    } else if (qualTesting && ra.charAt(0) == 'T') {
      return DatabaseConstants.RENTALAPPS_SOURCE_DASH_SYSTEM;
    }

    // Alphanumeric / contains letters

    return DatabaseConstants.RENTALAPPS_SOURCE_CWA_SYSTEM;
  }

  /** Returns true if the stall value is a purely numeric string. */
  public static boolean isNumericStall(String inputStall) {
    if (inputStall == null) {
      return false;
    }
    return inputStall.trim().matches("\\d+");
  }
}
