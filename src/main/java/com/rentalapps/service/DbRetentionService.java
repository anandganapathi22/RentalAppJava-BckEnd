package com.rentalapps.service;

import com.rentalapps.config.ApplicationConfig;
import com.rentalapps.exception.DatabaseException;
import com.rentalapps.util.DatabaseConstants;
import com.rentalapps.vo.GbCustomerBo;
import com.rentalapps.vo.GbCustomerReqObj;
import com.rentalapps.vo.GbCustomerRespObj;
import com.rentalapps.vo.GbLocationRespObj;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for deleting old customer records from the H2 database.
 */
@Component
public class DbRetentionService {

  private static final Logger logger = LoggerFactory.getLogger(DbRetentionService.class);

  private final SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");
  private final SimpleDateFormat format2 = new SimpleDateFormat("MM/dd/yyyy");

  @Autowired
  private ApplicationConfig appConfig;

  @Autowired
  private DbService dbService;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Transactional
  public List<GbCustomerRespObj> deleteOldData(String locationCode, String timeInterval)
      throws DatabaseException {

    logger.info("Deleting old customer data for location {}", locationCode);

    try {
      LocalDateTime dateTime = getLocalPastTime(locationCode, timeInterval);
      List<GbCustomerRespObj> customers = dbService.getCustomerList(locationCode);
      List<GbCustomerRespObj> deletedCustomers = new ArrayList<>();

      for (GbCustomerRespObj customer : customers) {
        LocalDateTime arrivalDateTime =
            formatArrivalDateTime(customer.getArrivalDate(), customer.getArrivalTime());
        Date custDate = parseDateTime(arrivalDateTime);
        Date compDate = parseDateTime(dateTime);

        if (custDate.before(compDate) || custDate.equals(compDate)) {
          deletedCustomers.add(customer);
          dbService.logShadowTable(
              toRequest(customer),
              DatabaseConstants.RENTALAPPS_OPERATION_DELETE,
              DatabaseConstants.RENTALAPPS_SOURCE_RETENTION_API);
          jdbcTemplate.update(
              "delete from " + quoted(appConfig.getDatabaseTable()) + " where \"id\" = ?",
              customer.getId());
        }
      }

      logger.info("Deleted {} customer(s) for location {}", deletedCustomers.size(), locationCode);
      return deletedCustomers;
    } catch (DatabaseException ex) {
      throw ex;
    } catch (Exception ex) {
      logger.error("Exception deleting old data", ex);
      throw new DatabaseException(
          DatabaseConstants.ERROR_TYPE_SYSTEM,
          DatabaseConstants.DATABASE_ERROR_MESSAGE4,
          ex.getMessage(),
          DatabaseConstants.HTTP_CODE_500);
    }
  }

  private LocalDateTime formatArrivalDateTime(String arrivalDate, String arrivalTime) throws ParseException {
    LocalDate arrivalDatePart = LocalDate.parse(format1.format(format2.parse(arrivalDate)));
    return LocalDateTime.of(arrivalDatePart, LocalTime.parse(arrivalTime, DateTimeFormatter.ofPattern("H:m")));
  }

  private Date parseDateTime(LocalDateTime dateTime) {
    return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
  }

  private LocalDateTime getLocalPastTime(String locationCode, String timeInterval) throws DatabaseException {
    String timeZone = getTimeZone(locationCode);
    if (StringUtils.isEmpty(timeZone)) {
      throw new DatabaseException(DatabaseConstants.ERROR_TYPE_SYSTEM,
          DatabaseConstants.DATABASE_ERROR_MESSAGE11,
          "Unable to get timezone from database", DatabaseConstants.HTTP_CODE_500);
    }
    return LocalDateTime.now(ZoneId.of(timeZone)).minusMinutes(Integer.parseInt(timeInterval));
  }

  private String getTimeZone(String locationCode) throws DatabaseException {
    List<GbLocationRespObj> locations = dbService.getLocation(locationCode);
    return locations.isEmpty() ? "" : locations.get(0).getTimeZone();
  }

  public void copyFields(GbCustomerReqObj t1, GbCustomerBo t2) {
    t1.setId(t2.getId());
    t1.setCustomerName(t2.getCustomerName());
    t1.setLocationCode(t2.getLocationCode());
    t1.setStall(t2.getStall());
    t1.setOneClub(t2.getOneClub());
    t1.setRa(t2.getra());
    t1.setArrivalDate(t2.getArrivalDate());
    t1.setArrivalTime(t2.getArrivalTime());
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

  private String quoted(String identifier) {
    return "\"" + StringUtils.defaultString(identifier).replace("\"", "\"\"") + "\"";
  }
}
