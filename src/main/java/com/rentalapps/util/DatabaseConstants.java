package com.rentalapps.util;



import java.time.format.DateTimeFormatter;

/**
 * Constants for database column names, error messages, HTTP codes,
 * date formatters, and Rental Apps business values used across the application.
 */
public class DatabaseConstants {
  public static final String ERROR_TYPE_APPLICATION = "Application Error";
  public static final String ERROR_TYPE_DATABASE = "Database Error";
  public static final String ERROR_TYPE_SYSTEM = "System Error";

  public static final String DATABASE_ERROR_MESSAGE1 = "Customer data creation failed in DB.";
  public static final String DATABASE_ERROR_MESSAGE2 = "Customer data updation failed in DB.";
  public static final String DATABASE_ERROR_MESSAGE3 = "Customer data deletion failed in DB.";
  public static final String DATABASE_ERROR_MESSAGE4 = "Customer data retrival failed in DB.";

  public static final String DATABASE_ERROR_MESSAGE5 = "Location data creation failed in DB.";
  public static final String DATABASE_ERROR_MESSAGE6 = "Location data updation failed in DB.";
  public static final String DATABASE_ERROR_MESSAGE7 = "Location data deletion failed in DB.";
  public static final String DATABASE_ERROR_MESSAGE8 = "Location data retrival failed in DB.";
  public static final String DATABASE_ERROR_MESSAGE9 = "No data found.";
  public static final String DATABASE_ERROR_MESSAGE10 = "Old customer data removal failed in DB.";
  public static final String DATABASE_ERROR_MESSAGE11 = "Timezone retrival failed in DB.";
 
  public static final String GET_LOCATION_SUCCESS_MESSAGE = "Location retrieved successfully.";
  public static final String ADD_LOCATION_SUCCESS_MESSAGE = "Location added successfully.";
  public static final String UPDATE_LOCATION_SUCCESS_MESSAGE = "Location updated successfully.";
  public static final String DELETE_LOCATION_SUCCESS_MESSAGE = "Location deleted successfully.";

  public static final String LOCALTIME_ERROR_MESSAGE = "Retrival of local time failed.";
  public static final String TIMEZONE_ERROR_MESSAGE = "Retrival of time zone failed.";
  public static final String LOCALTIME_SUCCESS_MESSAGE = "Local time retrieved successfully.";

  public static final String HTTP_CODE_200 = "200";
  public static final String HTTP_CODE_400 = "400";
  public static final String HTTP_CODE_403 = "403";
  public static final String HTTP_CODE_404 = "404";
  public static final String HTTP_CODE_500 = "500";
  
  public static final String[] ZONES = {"PRES CIR", "5 STAR", "GOLD", "ZONE 1", "ZONE 2", "ZONE 3", "COMPACT"};
  
  public static final String TIMEZONE_REF_lINK = "https://garygregory.wordpress.com/2013/06/18/what-are-the-java-timezone-ids/";
  
  public static final DateTimeFormatter DATE_FORMATTER_SOURCE = DateTimeFormatter.ofPattern("MM/dd/yyyy");
  public static final DateTimeFormatter TIME_FORMATTER_SOURCE = DateTimeFormatter.ofPattern("HH:mm");
  public static final DateTimeFormatter DATE_FORMATTER_TARGET = DateTimeFormatter.ofPattern("yyyy-MM-dd");
 
  public static final String REPORT_STATUS_ERROR = "ERROR";
  public static final String REPORT_STATUS_DELETED = "DELETED";
  public static final String REPORT_STATUS_NOTDELETED = "NOT DELETED";
  public static final String REPORT_ERROR_MESSAGE1 = "No timezone found for this location";

  public static final String RENTALAPPS_OPERATION_STATUS = "OPERATION";
  public static final String RENTALAPPS_OPERATION_TIME = "operationTime";
  public static final String RENTALAPPS_OPERATION_DATE = "operationDate";
  public static final String RENTALAPPS_OPERATION_ADD = "add";
  public static final String RENTALAPPS_OPERATION_DELETE = "delete";
  public static final String RENTALAPPS_OPERATION_UPDATE = "update";
  public static final String RENTALAPPS_SOURCE_CWA_SYSTEM = "CWA";
  public static final String RENTALAPPS_SOURCE_DASH_SYSTEM = "DASH";
  public static final String RENTALAPPS_SOURCE_TAS_SYSTEM = "TAS";
  public static final String RENTALAPPS_SOURCE_UI_SYSTEM = "COUNTER-DESK";
  public static final String RENTALAPPS_SOURCE_UI_DEFAULT_VAL = "RA12345";
  public static final String RENTALAPPS_SOURCE_SCHEDULER = "SCHEDULER";
  public static final String RENTALAPPS_SOURCE_RETENTION_API = "MANUAL-DATA-PURGE";

  public static final String RENTALAPPS_REGION_US = "US";
  public static final String RENTALAPPS_REGION_EU = "EU";

}
