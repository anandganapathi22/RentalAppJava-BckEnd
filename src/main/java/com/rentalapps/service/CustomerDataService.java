package com.rentalapps.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentalapps.config.ApplicationConfig;
import com.rentalapps.util.DatabaseConstants;
import com.rentalapps.exception.DatabaseException;
import com.rentalapps.service.DbRetentionService;
import com.rentalapps.service.DbService;
import com.rentalapps.vo.GbCustomerReqObj;
import com.rentalapps.vo.GbCustomerRespObj;
import com.rentalapps.vo.GbLocationReqObj;
import com.rentalapps.vo.GbLocationRespObj;
import com.rentalapps.exception.ServiceException;
import com.rentalapps.util.RentalDateTimeUtils;
import com.rentalapps.exception.ApplicationException;
import com.rentalapps.vo.CorrelationBean;
import com.rentalapps.vo.CustomerBean;
import com.rentalapps.vo.CustomerBeanLite;
import com.rentalapps.model.CwaMessageBean;
import com.rentalapps.model.EventType;
import com.rentalapps.vo.LocaltimeBean;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;



/**
 * Service layer orchestrating Rental Apps business logic including customer data retrieval,
 * persistence from MQ/UI, location management, name formatting, and correlation ID encoding.
 */
@Service
public class CustomerDataService {

  Logger logger = LoggerFactory.getLogger(CustomerDataService.class);
  ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  List<CustomerBean> custBeanList;
  List<CustomerBeanLite> custBeanListLite;

  private final DbService dbService;

  private final DbRetentionService dbRetentionService;

  private final ApplicationConfig appConfig;

  private final EventService<GbCustomerReqObj> eventService;

  public CustomerDataService(final DbService dbService,
                             final DbRetentionService dbRetentionService,
                             final ApplicationConfig appConfig,
                             final EventService<GbCustomerReqObj> eventService) {
    this.dbService = dbService;
    this.dbRetentionService = dbRetentionService;
    this.appConfig = appConfig;
    this.eventService = eventService;
  }

  /** Retrieves a lite (name + stall only) customer list for a location, sorted by name. */
  public List<CustomerBeanLite> getRentalAppsData(String locationId) throws IOException {
    // TODO Auto-generated method stub
    try {
      logger.info("Fetch data from DB for the location id:" + locationId);
      List<GbCustomerRespObj> data = dbService.getCustomerList(locationId);
      custBeanListLite = mapper.convertValue(data, new TypeReference<List<CustomerBeanLite>>() {
      });
      Collections.sort(custBeanListLite, Comparator.comparing(CustomerBeanLite::getCustomerName));
      logger.info("Customer List from DB:" + custBeanListLite.size());
      return custBeanListLite;
    } catch (Exception e) {
      //e.printStackTrace();
      logger.error("Exception caught:{}. Please check the resource path is correct "
            + "and the entries in the resource are comma separated and try again.", e.getMessage());
    }
    return new ArrayList<>();
  }
 
  /** Retrieves a full customer list for a location, sorted by name. */
  public List<CustomerBean> getRentalAppsData2(String locationId) throws IOException {
    // TODO Auto-generated method stub
    try {
      logger.info("Fetch data from DB for the location id:" + locationId);
      List<GbCustomerRespObj> data = dbService.getCustomerList(locationId);
      custBeanList = mapper.convertValue(data, new TypeReference<List<CustomerBean>>() {
      });
      Collections.sort(custBeanList, Comparator.comparing(CustomerBean::getCustomerName));
      logger.info("Customer List from DB:" + custBeanList.size());
      return custBeanList;
    } catch (Exception e) {
      //e.printStackTrace();
      logger.error("Exception caught:{}. Please check the resource path is correct "
            + "and the entries in the resource are comma separated and try again.", e.getMessage());
    }
    return new ArrayList<>();
  }
  
  /** Deletes old customer data for a location based on a time interval threshold. */
  public List<CustomerBean> deleteOldData(String locationId, String timeInterval) 
          throws IOException, ServiceException {
    // TODO Auto-generated method stub
    try {    
      logger.info("Fetch data from DB for the location id:" + locationId 
              + ", For dataretention of minutes:" + timeInterval);
      List<GbCustomerRespObj> data = dbRetentionService.deleteOldData(locationId, timeInterval);
      custBeanList = mapper.convertValue(data, new TypeReference<List<CustomerBean>>() {
      });
      Collections.sort(custBeanList, Comparator.comparing(CustomerBean::getCustomerName));
      logger.info("Customer List from DB:" + custBeanList.size());
      return custBeanList;
    } catch (Exception e) {
      //e.printStackTrace();
      logger.error("Exception caught:{}. Please check the resource path is correct "
            + "and the entries in the resource are comma separated and try again.", e.getMessage());
      throw new ServiceException(DatabaseConstants.ERROR_TYPE_APPLICATION,
              DatabaseConstants.DATABASE_ERROR_MESSAGE11,
              DatabaseConstants.DATABASE_ERROR_MESSAGE9, DatabaseConstants.HTTP_CODE_404);
    }
  }

  /** Retrieves a single location by its code. */
  public List<GbLocationRespObj> getLocation(String locationId) 
      throws DatabaseException, ServiceException {
    
    logger.info("Start of com.rentalapps.service.getLocation () ...");
    
    List<GbLocationRespObj> resp = dbService.getLocation(locationId.trim().toUpperCase());
    
    if (resp == null) {
      throw new ServiceException(DatabaseConstants.ERROR_TYPE_APPLICATION,
          DatabaseConstants.DATABASE_ERROR_MESSAGE8,
          DatabaseConstants.DATABASE_ERROR_MESSAGE9, DatabaseConstants.HTTP_CODE_404);
    }
    logger.info("End of com.rentalapps.service.getLocation () ...");
    return resp;
  }

  /** Retrieves multiple locations by their codes. */
  public List<GbLocationRespObj> getLocationList(List<String> locationIds) 
      throws DatabaseException, ServiceException {
    
    logger.info("Start of com.rentalapps.service.getLocationList () ...");
    
    List<GbLocationRespObj> resp = dbService.getLocationList(locationIds);
    
    if (resp == null || resp.size() == 0) {
      throw new ServiceException(DatabaseConstants.ERROR_TYPE_APPLICATION,
          DatabaseConstants.DATABASE_ERROR_MESSAGE8,
          DatabaseConstants.DATABASE_ERROR_MESSAGE9, DatabaseConstants.HTTP_CODE_404);
    }
    logger.info("End of com.rentalapps.service.getLocationList () ...");
    return resp;
  }

  /** Adds a new location record. */
  public GbLocationRespObj addLocation(GbLocationReqObj inputLocation)
      throws DatabaseException {
  
    GbLocationRespObj result = dbService.addLocation(inputLocation);
    return result;
  }

  /** Updates an existing location record. */
  public GbLocationRespObj updateLocation(GbLocationReqObj inputLocation)
      throws DatabaseException {
  
    GbLocationRespObj result = dbService.updateLocation(inputLocation);
    return result;
  }

  /** Removes a location record. */
  public GbLocationRespObj removeLocation(GbLocationReqObj inputLocation)
      throws DatabaseException {
  
    GbLocationRespObj result = dbService.removeLocation(inputLocation);
    return result;
  }

  /**
   * Processes queue-sourced rental data: formats arrival dates and customer names
   * by region, then delegates to persistData for DB operations.
   */
  public void persistQueueData(String locationCd, CwaMessageBean cwaMessageBean)
        throws IllegalArgumentException, IOException, ApplicationException {
    cwaMessageBean.rental().stream().forEach(rental -> {
      /* Process customer name received from the queue with update or delete action.
       * the customer name to be transformed to GB expected format "LastName F."
       */
      rental.setArrivalDate(RentalDateTimeUtils.getTwoDigitDayMonth(rental.getArrivalDate()));
      if (!StringUtils.trimToEmpty(rental.getAction()).equalsIgnoreCase(DatabaseConstants.RENTALAPPS_OPERATION_ADD)) {
        rental.setCustomerName(
                    formatNameByRegion(
                            StringUtils.trimToEmpty(rental.getCustomerName()).toUpperCase(),
                            locationCd));
      }
      logger.info("persistQueueData::" + rental.getAction() + "::" + rental.getCustomerName() 
          + "::" + rental.getArrivalDate());
    });
    persistData(locationCd, cwaMessageBean);  
  }

  /**
   * Persists rental data to the database and records corresponding local events.
   */
  public void persistData(String locationCd, CwaMessageBean cwaMessageBean)
            throws IllegalArgumentException, IOException, ApplicationException {

    logger.info("cwaMsBean::" + cwaMessageBean.toString());
    cwaMessageBean.rental().stream()
                .forEach(
                        rental -> {
                    try {
                      logger.info("persistData::rental::" + rental.getCustomerName());
                      rental.setLocationCode(StringUtils.trimToEmpty(locationCd));
                      rental.setId(
                          StringUtils.trimToEmpty(rental.getLocationCode())
                                                + "#"
                                                + StringUtils.trimToEmpty(rental.getCustomerName())
                                                + "#"
                                                + StringUtils.trimToEmpty(rental.getOneClub()));
                      logger.info("rental::" + rental.toString());
                      GbCustomerReqObj requestObj = mapper.convertValue(rental, GbCustomerReqObj.class);

                      if (StringUtils.trimToEmpty(rental.getAction())
                              .equalsIgnoreCase(DatabaseConstants.RENTALAPPS_OPERATION_DELETE)) {
                        logger.info("Deleting Customer in DB -->" + rental.getAction());
                        this.eventService.send(EventType.RENTAL_APPS_DELETE_EVENT, requestObj);
                        dbService.removeCustomer(requestObj);
                      } else if (StringUtils.trimToEmpty(rental.getAction())
                              .equalsIgnoreCase(DatabaseConstants.RENTALAPPS_OPERATION_ADD)) {
                        logger.info("Adding Customer in DB -->" + rental.getAction());
                        /* Add action from UI & MQ need to be handled same way by transforming to format "LastName F."
                        * MQ : LastName FirstName =>  LastName F.
                        * Admin UI : LastName F => LastName F.
                        */
                        rental.setCustomerName(StringUtils
                                  .trimToEmpty(formatNameByRegion(
                                          StringUtils.trimToEmpty(rental.getCustomerName()).toUpperCase(),
                                          locationCd)));
                        rental.setStall(changeStallIfAsterisk(rental.getStall()));
                        rental.setId(StringUtils.trimToEmpty(
                                            rental.getLocationCode())
                                                    + "#"
                                                    + StringUtils.trimToEmpty(rental.getCustomerName())
                                                    + "#"
                                                    + StringUtils.trimToEmpty(rental.getOneClub()));
                        requestObj = mapper.convertValue(rental, GbCustomerReqObj.class);

                        this.eventService.send(EventType.RENTAL_APPS_ADD_EVENT, requestObj);
                        dbService.addCustomer(requestObj);
                        logger.info("Customer added::" + rental.getCustomerName());
                      } else {
                          //if(rental.getAction().equalsIgnoreCase("edit"))
                          logger.info("Updating Customer in DB -->" + rental.getAction());
                          dbService.updateCustomer(requestObj);
                          this.eventService.send(EventType.RENTAL_APPS_UPDATE_EVENT, requestObj);
                      }

                    } catch (DatabaseException e) {
                      logger.error("Database exception is ", e);
                    }
                        });
  }

  /** Formats a customer name based on region (US vs EU naming conventions). */
  private String formatNameByRegion(String custName, String locationCode) {

    if (dbService.isEuropeLocation(locationCode)) {
      return formatEuropeanName(custName);
    }
    return formatName(custName); // existing US logic
  }

  /** Formats a US customer name to "LastName F." format. */
  private String formatName(String custName) {
    if (custName == null || custName.trim().isEmpty()) {
      return custName;
    }

    String[] splitName = custName.trim().split("\\s+");
    String processedName;

    int lengthCutInt = Integer.parseInt(appConfig.getFirstNameLengthCut().trim());
    logger.info("lengthCutInt = " + lengthCutInt);

    if (splitName.length > 1) {
      String firstName = splitName[0];
      String lastPart = splitName[splitName.length - 1];

      int cut = Math.min(lengthCutInt, lastPart.length());
      processedName = firstName + " " + lastPart.substring(0, cut) + ".";
    } else {
      processedName = splitName[0];
    }

    logger.info("receivedName::" + custName);
    logger.info("processedName::" + processedName);
    return processedName;
  }

  /** Formats a European customer name preserving multi-word surnames: "Surname F." */
  private String formatEuropeanName(String custName) {

    if (custName == null || custName.trim().isEmpty()) {
      return custName;
    }

    String normalizedName = custName.trim().replaceAll("\\s+", " ");
    String[] splitName = normalizedName.split("\\s+");

    logger.info("EU receivedName::{}", custName);

    // Already formatted surname
    if (splitName.length >= 2 && splitName[splitName.length - 1].matches("[A-Z]\\.")) {
      return normalizedName;
    }

    if (splitName.length == 1) {
      return splitName[0];
    }

    int lengthCutInt = Integer.parseInt(appConfig.getFirstNameLengthCut().trim());

    // EU logic ÃƒÂ¢Ã¢â‚¬Â Ã¢â‚¬â„¢ everything except last word is surname
    String surname = String.join(" ",
                java.util.Arrays.copyOfRange(splitName, 0, splitName.length - 1));

    String firstName = splitName[splitName.length - 1];
    int cut = Math.min(lengthCutInt, firstName.length());
    String processedName = surname + " " + firstName.substring(0, cut) + ".";
    logger.info("EU processedName::{}", processedName);
    return processedName;
  }

  /** Replaces stall values containing '*' with "SEE DESK". */
  private String changeStallIfAsterisk(String stall) {

    if (StringUtils.isNotEmpty(stall) && StringUtils.contains(stall, "*")) {
      return "SEE DESK";
    } else {
      return stall;
    }
  }
  
  /** Generates a hex-encoded correlation ID from a location code. */
  public CorrelationBean getCorrelationId(String locationId) {
    CorrelationBean bean = new CorrelationBean();
    try {
      bean.setCorrelationId(Hex.encodeHexString(locationId.trim()
          .toUpperCase().getBytes("ISO-8859-1")) + "2020202020202020202020202020202020");
    } catch (UnsupportedEncodingException ex) {
      logger.error("Exception in getLocationData() method", ex.getMessage());
    }
        
    bean.setHertzLocationCode(locationId.trim().toUpperCase());
    return bean;
  }
  
  /** Decodes a hex-encoded correlation ID back to a location code. */
  public CorrelationBean getLocationFromCorrelationId(String correlationId) {
    CorrelationBean bean = new CorrelationBean();
    try {
      bean.setHertzLocationCode(new String(Hex.decodeHex(correlationId
          .toCharArray()), StandardCharsets.UTF_8).trim());
    } catch (DecoderException ex) {
      logger.error("Exception in getLocationFromCorrelationId() method", ex.getMessage());
    }
    bean.setCorrelationId(correlationId);
    return bean;
  }
  
  /** Returns local time, timezone, and reference info for a given continent and city. */
  public LocaltimeBean getLocalTime(String continent, String city)
      throws ServiceException {
    LocaltimeBean bean = new LocaltimeBean();
    bean.setContinent(continent);
    bean.setCity(city);
    bean.setLocalDateTime(RentalDateTimeUtils.getCurrentLocalTime(continent, city));
    bean.setTimeZone(RentalDateTimeUtils.getTimeZone(continent, city));
    bean.setReference(DatabaseConstants.TIMEZONE_REF_lINK);
    return bean;
  }
}
