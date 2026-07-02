package com.rentalapps.service;

import com.rentalapps.config.ApplicationConfig;
import com.rentalapps.exception.DatabaseException;
import com.rentalapps.util.DatabaseConstants;
import com.rentalapps.util.RentalDateTimeUtils;
import com.rentalapps.vo.GbCustomerBo;
import com.rentalapps.vo.GbCustomerReqObj;
import com.rentalapps.vo.GbCustomerRespObj;
import com.rentalapps.vo.GbLocationReqObj;
import com.rentalapps.vo.GbLocationRespObj;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * JDBC-backed database service for Rental Apps customer, audit, and location data.
 */
@Component
public class DbService {

  private static final Logger logger = LoggerFactory.getLogger(DbService.class);

  @Autowired
  private ApplicationConfig appConfig;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  private final ConcurrentHashMap<String, String> locationTimeZoneCache = new ConcurrentHashMap<>();

  private final RowMapper<GbCustomerRespObj> customerMapper = (rs, rowNum) -> {
    GbCustomerRespObj customer = new GbCustomerRespObj();
    customer.setId(rs.getString("id"));
    customer.setCustomerName(rs.getString("customerName"));
    customer.setLocationCode(rs.getString("locationCode"));
    customer.setStall(rs.getString("stall"));
    customer.setOneClub(rs.getString("oneClub"));
    customer.setRa(rs.getString("ra"));
    customer.setArrivalDate(rs.getString("arrivalDate"));
    customer.setArrivalTime(rs.getString("arrivalTime"));
    customer.setCreatedDateTime(rs.getString("createdDatetime"));
    customer.setUpdatedDateTime(rs.getString("updatedDatetime"));
    customer.setIdentifier(RentalDateTimeUtils.isZone(StringUtils.defaultString(customer.getStall())) ? "zone" : "stall");
    return customer;
  };

  private final RowMapper<GbLocationRespObj> locationMapper = (rs, rowNum) -> {
    GbLocationRespObj location = new GbLocationRespObj();
    location.setHertzLocationCode(rs.getString("hertzLocationCode"));
    location.setDisplayName(rs.getString("displayName"));
    location.setTimeZone(rs.getString("timeZone"));
    return location;
  };

  @Transactional
  public GbCustomerRespObj addCustomer(GbCustomerReqObj inputCustomer) throws DatabaseException {
    logger.info("Adding customer id={}", inputCustomer.getId());

    String locationCode = normalizeLocation(inputCustomer.getLocationCode());
    String ra = normalizeUpper(inputCustomer.getRa());
    String source = sourceFor(ra, locationCode);
    String now = RentalDateTimeUtils.getCurrentUtcTime();

    String sql = "insert into " + customerTable()
        + " (\"id\", \"customerName\", \"locationCode\", \"stall\", \"oneClub\", \"ra\", "
        + "\"sourceSystem\", \"arrivalDate\", \"arrivalTime\", \"createdDatetime\", \"updatedDatetime\") "
        + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    try {
      jdbcTemplate.update(sql,
          trim(inputCustomer.getId()),
          trim(inputCustomer.getCustomerName()),
          locationCode,
          trim(inputCustomer.getStall()),
          trim(inputCustomer.getOneClub()),
          ra,
          source,
          trim(inputCustomer.getArrivalDate()),
          trim(inputCustomer.getArrivalTime()),
          now,
          now);

      logShadowTable(inputCustomer, DatabaseConstants.RENTALAPPS_OPERATION_ADD, null);
      return getCustomer(trim(inputCustomer.getId()));
    } catch (DuplicateKeyException ex) {
      logger.info("Customer already exists. Redirecting add to update for id={}", inputCustomer.getId());
      return validateUpdateCustomer(inputCustomer);
    } catch (DataAccessException ex) {
      throw databaseException(DatabaseConstants.DATABASE_ERROR_MESSAGE1, ex, DatabaseConstants.HTTP_CODE_400);
    } catch (Exception ex) {
      throw systemException(DatabaseConstants.DATABASE_ERROR_MESSAGE1, ex);
    }
  }

  @Transactional
  public GbCustomerRespObj updateCustomer(GbCustomerReqObj inputCustomer) throws DatabaseException {
    logger.info("Updating customer id={}", inputCustomer.getId());

    try {
      requireExistingCustomer(trim(inputCustomer.getId()), DatabaseConstants.DATABASE_ERROR_MESSAGE2);

      List<String> assignments = new ArrayList<>();
      List<Object> args = new ArrayList<>();

      addAssignment(assignments, args, "customerName", inputCustomer.getCustomerName(), false);
      addAssignment(assignments, args, "locationCode", inputCustomer.getLocationCode(), true);
      addAssignment(assignments, args, "stall", inputCustomer.getStall(), false);
      addAssignment(assignments, args, "oneClub", inputCustomer.getOneClub(), false);
      addAssignment(assignments, args, "arrivalDate", inputCustomer.getArrivalDate(), false);
      addAssignment(assignments, args, "arrivalTime", inputCustomer.getArrivalTime(), false);

      if (StringUtils.isNotBlank(inputCustomer.getRa())) {
        String ra = normalizeUpper(inputCustomer.getRa());
        assignments.add("\"ra\" = ?");
        args.add(ra);
        assignments.add("\"sourceSystem\" = ?");
        args.add(sourceFor(ra, normalizeLocation(inputCustomer.getLocationCode())));
      }

      assignments.add("\"updatedDatetime\" = ?");
      args.add(RentalDateTimeUtils.getCurrentUtcTime());
      args.add(trim(inputCustomer.getId()));

      jdbcTemplate.update(
          "update " + customerTable() + " set " + String.join(", ", assignments) + " where \"id\" = ?",
          args.toArray());

      logShadowTable(inputCustomer, DatabaseConstants.RENTALAPPS_OPERATION_UPDATE, null);
      return getCustomer(trim(inputCustomer.getId()));
    } catch (DatabaseException ex) {
      throw ex;
    } catch (DataAccessException ex) {
      throw databaseException(DatabaseConstants.DATABASE_ERROR_MESSAGE2, ex, DatabaseConstants.HTTP_CODE_400);
    } catch (Exception ex) {
      throw systemException(DatabaseConstants.DATABASE_ERROR_MESSAGE2, ex);
    }
  }

  @Transactional
  public GbCustomerRespObj removeCustomer(GbCustomerReqObj inputCustomer) throws DatabaseException {
    if (inputCustomer == null || inputCustomer.getId() == null || inputCustomer.getRa() == null) {
      throw new DatabaseException(
          DatabaseConstants.ERROR_TYPE_DATABASE,
          "Invalid delete request",
          "Customer id and ra must be provided",
          DatabaseConstants.HTTP_CODE_400);
    }

    try {
      GbCustomerRespObj existing = getCustomer(trim(inputCustomer.getId()));
      if (existing == null || !StringUtils.equalsIgnoreCase(trim(existing.getRa()), trim(inputCustomer.getRa()))) {
        logger.info("Delete skipped due to id/ra mismatch for id={}", inputCustomer.getId());
        return null;
      }

      int deleted = jdbcTemplate.update(
          "delete from " + customerTable() + " where \"id\" = ?",
          trim(inputCustomer.getId()));
      if (deleted == 0) {
        return null;
      }

      logShadowTable(toRequest(existing), DatabaseConstants.RENTALAPPS_OPERATION_DELETE, null);
      return existing;
    } catch (DatabaseException ex) {
      throw ex;
    } catch (DataAccessException ex) {
      throw databaseException(DatabaseConstants.DATABASE_ERROR_MESSAGE3, ex, DatabaseConstants.HTTP_CODE_400);
    } catch (Exception ex) {
      throw systemException(DatabaseConstants.DATABASE_ERROR_MESSAGE3, ex);
    }
  }

  public boolean isEuropeLocation(String locationCode) {
    String normalizedCode = normalizeLocation(locationCode);
    if (StringUtils.isBlank(normalizedCode)) {
      return false;
    }

    String timeZone = locationTimeZoneCache.get(normalizedCode);
    if (timeZone == null) {
      try {
        List<GbLocationRespObj> locations = getLocation(normalizedCode);
        if (locations.isEmpty() || StringUtils.isBlank(locations.get(0).getTimeZone())) {
          return false;
        }
        timeZone = locations.get(0).getTimeZone();
        locationTimeZoneCache.put(normalizedCode, timeZone);
      } catch (DatabaseException ex) {
        logger.warn("Unable to determine timezone for location {}", normalizedCode, ex);
        return false;
      }
    }

    return timeZone.startsWith("Europe/");
  }

  public GbCustomerRespObj getCustomer(String id) throws DatabaseException {
    try {
      return jdbcTemplate.queryForObject(
          customerSelect() + " where \"id\" = ?",
          customerMapper,
          trim(id));
    } catch (EmptyResultDataAccessException ex) {
      return null;
    } catch (DataAccessException ex) {
      throw databaseException(DatabaseConstants.DATABASE_ERROR_MESSAGE4, ex, DatabaseConstants.HTTP_CODE_400);
    }
  }

  public List<GbCustomerRespObj> getCustomerList() throws DatabaseException {
    try {
      return jdbcTemplate.query(customerSelect() + " order by \"customerName\"", customerMapper);
    } catch (DataAccessException ex) {
      throw databaseException(DatabaseConstants.DATABASE_ERROR_MESSAGE4, ex, DatabaseConstants.HTTP_CODE_400);
    }
  }

  public List<GbCustomerRespObj> getCustomerList(String locationCode) throws DatabaseException {
    try {
      return jdbcTemplate.query(
          customerSelect() + " where \"locationCode\" = ? order by \"customerName\"",
          customerMapper,
          normalizeLocation(locationCode));
    } catch (DataAccessException ex) {
      throw databaseException(DatabaseConstants.DATABASE_ERROR_MESSAGE4, ex, DatabaseConstants.HTTP_CODE_400);
    }
  }

  public List<GbLocationRespObj> getLocation(String hertzLocationCode) throws DatabaseException {
    try {
      return jdbcTemplate.query(
          locationSelect() + " where \"hertzLocationCode\" = ?",
          locationMapper,
          normalizeLocation(hertzLocationCode));
    } catch (DataAccessException ex) {
      throw databaseException(DatabaseConstants.DATABASE_ERROR_MESSAGE8, ex, DatabaseConstants.HTTP_CODE_400);
    }
  }

  public List<GbLocationRespObj> getLocationList(List<String> hertzLocationCodes) throws DatabaseException {
    if (hertzLocationCodes == null || hertzLocationCodes.isEmpty()) {
      return Collections.emptyList();
    }

    List<String> codes = hertzLocationCodes.stream()
        .map(this::normalizeLocation)
        .filter(StringUtils::isNotBlank)
        .toList();
    if (codes.isEmpty()) {
      return Collections.emptyList();
    }

    String placeholders = String.join(", ", Collections.nCopies(codes.size(), "?"));
    try {
      return jdbcTemplate.query(
          locationSelect() + " where \"hertzLocationCode\" in (" + placeholders + ") order by \"hertzLocationCode\"",
          locationMapper,
          codes.toArray());
    } catch (DataAccessException ex) {
      throw databaseException(DatabaseConstants.DATABASE_ERROR_MESSAGE8, ex, DatabaseConstants.HTTP_CODE_400);
    }
  }

  @Transactional
  public GbLocationRespObj addLocation(GbLocationReqObj inputLocation) throws DatabaseException {
    String code = normalizeLocation(inputLocation.getHertzLocationCode());
    try {
      jdbcTemplate.update(
          "insert into " + locationTable()
              + " (\"hertzLocationCode\", \"displayName\", \"timeZone\") values (?, ?, ?)",
          code,
          trim(inputLocation.getDisplayName()),
          trim(inputLocation.getTimeZone()));
      refreshLocationCache(code, inputLocation.getTimeZone());
      return getLocation(code).stream().findFirst().orElse(null);
    } catch (DataAccessException ex) {
      throw databaseException(DatabaseConstants.DATABASE_ERROR_MESSAGE5, ex, DatabaseConstants.HTTP_CODE_400);
    }
  }

  @Transactional
  public GbLocationRespObj updateLocation(GbLocationReqObj inputLocation) throws DatabaseException {
    String code = normalizeLocation(inputLocation.getHertzLocationCode());
    try {
      GbLocationRespObj existing = getLocation(code).stream().findFirst().orElse(null);
      if (existing == null) {
        throw new EmptyResultDataAccessException(1);
      }

      List<String> assignments = new ArrayList<>();
      List<Object> args = new ArrayList<>();
      addAssignment(assignments, args, "displayName", inputLocation.getDisplayName(), false);
      addAssignment(assignments, args, "timeZone", inputLocation.getTimeZone(), false);
      if (assignments.isEmpty()) {
        return existing;
      }

      args.add(code);
      jdbcTemplate.update(
          "update " + locationTable() + " set " + String.join(", ", assignments)
              + " where \"hertzLocationCode\" = ?",
          args.toArray());
      refreshLocationCache(code, inputLocation.getTimeZone());
      return existing;
    } catch (DataAccessException ex) {
      throw databaseException(DatabaseConstants.DATABASE_ERROR_MESSAGE6, ex, DatabaseConstants.HTTP_CODE_400);
    }
  }

  @Transactional
  public GbLocationRespObj removeLocation(GbLocationReqObj inputLocation) throws DatabaseException {
    String code = normalizeLocation(inputLocation.getHertzLocationCode());
    try {
      GbLocationRespObj existing = getLocation(code).stream().findFirst().orElse(null);
      if (existing == null) {
        throw new EmptyResultDataAccessException(1);
      }

      jdbcTemplate.update("delete from " + locationTable() + " where \"hertzLocationCode\" = ?", code);
      locationTimeZoneCache.remove(code);
      return existing;
    } catch (DataAccessException ex) {
      throw databaseException(DatabaseConstants.DATABASE_ERROR_MESSAGE7, ex, DatabaseConstants.HTTP_CODE_400);
    }
  }

  @Transactional
  public void removeOldCustomerData() throws DatabaseException {
    try {
      Map<String, String> allLocations = new HashMap<>();
      jdbcTemplate.query(locationSelect(), locationMapper)
          .forEach(location -> allLocations.put(location.getHertzLocationCode(), location.getTimeZone()));

      long measureOld = Long.parseLong(appConfig.getTimeWindow());
      String deletionRegion = appConfig.getDeletionRegion();
      int totalRecords = 0;
      int totalDeletedRecords = 0;

      for (GbCustomerRespObj customer : getCustomerList()) {
        String timeZone = allLocations.get(customer.getLocationCode());
        if (StringUtils.isBlank(timeZone) || StringUtils.isBlank(customer.getArrivalDate())
            || StringUtils.isBlank(customer.getArrivalTime())) {
          continue;
        }

        boolean europeLocation = timeZone.startsWith("Europe/");
        if (DatabaseConstants.RENTALAPPS_REGION_EU.equalsIgnoreCase(deletionRegion) && !europeLocation) {
          continue;
        }
        if (DatabaseConstants.RENTALAPPS_REGION_US.equalsIgnoreCase(deletionRegion) && europeLocation) {
          continue;
        }

        totalRecords++;
        if (StringUtils.isNotBlank(RentalDateTimeUtils.isValidDate(customer.getArrivalDate()))
            || StringUtils.isNotBlank(RentalDateTimeUtils.isValidTime(customer.getArrivalTime()))) {
          continue;
        }

        LocalDateTime arrivalDateTime =
            RentalDateTimeUtils.formatArrivalDateTime(customer.getArrivalDate(), customer.getArrivalTime());
        LocalDateTime currentDateTime = RentalDateTimeUtils.getCurrentLocalTime(timeZone);
        long diffInMinutes = ChronoUnit.MINUTES.between(arrivalDateTime, currentDateTime);

        if (diffInMinutes >= measureOld) {
          jdbcTemplate.update("delete from " + customerTable() + " where \"id\" = ?", customer.getId());
          totalDeletedRecords++;
          logShadowTable(
              toRequest(customer),
              DatabaseConstants.RENTALAPPS_OPERATION_DELETE,
              DatabaseConstants.RENTALAPPS_SOURCE_SCHEDULER + "_" + deletionRegion);
        }
      }

      logger.info("{} record(s) deleted out of {} record(s) processed for region {}",
          totalDeletedRecords, totalRecords, deletionRegion);
    } catch (Exception ex) {
      throw systemException(DatabaseConstants.DATABASE_ERROR_MESSAGE10, ex);
    }
  }

  public void copyFields(GbCustomerRespObj gbCustomerRespObj, GbCustomerBo gbCustomerBo) {
    gbCustomerRespObj.setId(gbCustomerBo.getId());
    gbCustomerRespObj.setCustomerName(gbCustomerBo.getCustomerName());
    gbCustomerRespObj.setLocationCode(gbCustomerBo.getLocationCode());
    gbCustomerRespObj.setStall(gbCustomerBo.getStall());
    gbCustomerRespObj.setOneClub(gbCustomerBo.getOneClub());
    gbCustomerRespObj.setRa(gbCustomerBo.getra());
    gbCustomerRespObj.setArrivalDate(gbCustomerBo.getArrivalDate());
    gbCustomerRespObj.setArrivalTime(gbCustomerBo.getArrivalTime());
    gbCustomerRespObj.setCreatedDateTime(gbCustomerBo.getCreatedDatetime());
    gbCustomerRespObj.setUpdatedDateTime(gbCustomerBo.getUpdatedDatetime());
  }

  void logShadowTable(GbCustomerReqObj inputCustomer, String operation, String sourceOverride) {
    if (!appConfig.isShadowTableEnable()) {
      return;
    }

    try {
      String operationTime = RentalDateTimeUtils.getCurrentUtcTime() + "#" + System.nanoTime();
      String operationDate = operationTime.length() >= 10 ? operationTime.substring(0, 10) : null;
      String source = sourceOverride != null
          ? sourceOverride
          : sourceFor(inputCustomer.getRa(), inputCustomer.getLocationCode());
      long expiresAt = java.time.ZonedDateTime.now(ZoneOffset.UTC)
          .plusMonths(appConfig.getShadowTableExpiryMonths())
          .toEpochSecond();

      jdbcTemplate.update(
          "insert into " + shadowTable()
              + " (\"id\", \"operationTime\", \"operationDate\", \"customerName\", \"locationCode\", "
              + "\"stall\", \"oneClub\", \"ra\", \"sourceSystem\", \"arrivalDate\", \"arrivalTime\", "
              + "\"updatedDatetime\", \"OPERATION\", \"expiresAt\") values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
          trim(inputCustomer.getId()),
          operationTime,
          operationDate,
          trim(inputCustomer.getCustomerName()),
          normalizeLocation(inputCustomer.getLocationCode()),
          trim(inputCustomer.getStall()),
          trim(inputCustomer.getOneClub()),
          normalizeUpper(inputCustomer.getRa()),
          source,
          trim(inputCustomer.getArrivalDate()),
          trim(inputCustomer.getArrivalTime()),
          operationTime,
          operation,
          expiresAt);
    } catch (Exception ex) {
      logger.error("Failed to log audit operation {}", operation, ex);
    }
  }

  private GbCustomerRespObj validateUpdateCustomer(GbCustomerReqObj inputCustomer) throws DatabaseException {
    return updateCustomer(inputCustomer);
  }

  private void requireExistingCustomer(String id, String message) throws DatabaseException {
    if (getCustomer(id) == null) {
      throw new DatabaseException(
          DatabaseConstants.ERROR_TYPE_DATABASE,
          message,
          "Customer not found: " + id,
          DatabaseConstants.HTTP_CODE_400);
    }
  }

  private void addAssignment(List<String> assignments, List<Object> args, String column, String value,
      boolean uppercase) {
    if (StringUtils.isNotBlank(value)) {
      assignments.add(quote(column) + " = ?");
      args.add(uppercase ? normalizeUpper(value) : trim(value));
    }
  }

  private void refreshLocationCache(String locationCode, String timeZone) {
    String normalizedCode = normalizeLocation(locationCode);
    if (StringUtils.isBlank(normalizedCode)) {
      return;
    }
    if (StringUtils.isBlank(timeZone)) {
      locationTimeZoneCache.remove(normalizedCode);
    } else {
      locationTimeZoneCache.put(normalizedCode, trim(timeZone));
    }
  }

  private GbCustomerReqObj toRequest(GbCustomerRespObj customer) {
    GbCustomerReqObj request = new GbCustomerReqObj();
    request.setId(customer.getId());
    request.setCustomerName(customer.getCustomerName());
    request.setLocationCode(customer.getLocationCode());
    request.setStall(customer.getStall());
    request.setOneClub(customer.getOneClub());
    request.setRa(customer.getRa());
    request.setArrivalDate(customer.getArrivalDate());
    request.setArrivalTime(customer.getArrivalTime());
    return request;
  }

  private String sourceFor(String ra, String locationCode) {
    if (isEuropeLocation(locationCode)) {
      return DatabaseConstants.RENTALAPPS_SOURCE_TAS_SYSTEM;
    }
    return RentalDateTimeUtils.getSourceSystem(trim(ra), appConfig.isEnableQualTesting());
  }

  private String customerSelect() {
    return "select \"id\", \"customerName\", \"locationCode\", \"stall\", \"oneClub\", \"ra\", "
        + "\"arrivalDate\", \"arrivalTime\", \"createdDatetime\", \"updatedDatetime\" from " + customerTable();
  }

  private String locationSelect() {
    return "select \"hertzLocationCode\", \"displayName\", \"timeZone\" from " + locationTable();
  }

  private String customerTable() {
    return quote(appConfig.getDatabaseTable());
  }

  private String shadowTable() {
    return quote(appConfig.getShadowTable());
  }

  private String locationTable() {
    return quote(appConfig.getLocationTable());
  }

  private String quote(String identifier) {
    return "\"" + StringUtils.defaultString(identifier).replace("\"", "\"\"") + "\"";
  }

  private String trim(String value) {
    return StringUtils.trimToEmpty(value);
  }

  private String normalizeUpper(String value) {
    return trim(value).toUpperCase();
  }

  private String normalizeLocation(String value) {
    return normalizeUpper(value);
  }

  private DatabaseException databaseException(String message, Exception ex, String httpCode) {
    logger.error(message, ex);
    return new DatabaseException(DatabaseConstants.ERROR_TYPE_DATABASE, message, ex.getMessage(), httpCode);
  }

  private DatabaseException systemException(String message, Exception ex) {
    logger.error(message, ex);
    return new DatabaseException(DatabaseConstants.ERROR_TYPE_SYSTEM, message, ex.getMessage(), DatabaseConstants.HTTP_CODE_500);
  }
}
