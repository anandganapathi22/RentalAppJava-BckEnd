package com.rentalapps.controller;

import com.rentalapps.util.DatabaseConstants;
import com.rentalapps.exception.DatabaseException;
import com.rentalapps.vo.GbLocationReqObj;
import com.rentalapps.vo.GbLocationRespObj;
import com.rentalapps.exception.ServiceException;
import com.rentalapps.vo.SuccessRespObj;
import com.rentalapps.exception.ApplicationException;
import com.rentalapps.exception.NoContentException;
import com.rentalapps.vo.CorrelationBean;
import com.rentalapps.vo.AiCustomerQueryRequest;
import com.rentalapps.vo.CustomerBean;
import com.rentalapps.vo.CustomerBeanLite;
import com.rentalapps.model.CwaMessageBean;
import com.rentalapps.vo.AiCustomerQueryResponse;
import com.rentalapps.vo.LocaltimeBean;
import com.rentalapps.model.Rental;
import com.rentalapps.service.CustomerAiQueryService;
import com.rentalapps.service.CustomerDataService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing Rental Apps API endpoints for customer stall details,
 * adhoc changes, location management, data retention, and timezone utilities.
 */
@RestController
public class RentalAppsController {
  Logger logger = LoggerFactory.getLogger(RentalAppsController.class);
  List<CustomerBean> custBeanList;
  List<CustomerBeanLite> custBeanListLite;

  @Autowired
  private CustomerDataService customerDataService;

  @Autowired
  private CustomerAiQueryService customerAiQueryService;
  
  /** GET /StallDetails - Returns a lite list of customer stall assignments for a location. */
  @RequestMapping(value = "/StallDetails", method = RequestMethod.GET, produces = "application/json")
  public ResponseEntity<Object> availableStallsinLocation(@RequestParam(required = true) String locationId)
        throws NoContentException, IOException {
    logger.info("GetStallDetails::location:->{}", locationId);
    custBeanListLite = customerDataService.getRentalAppsData(locationId);
    return new ResponseEntity<>(custBeanListLite, HttpStatus.OK);
  }

  /** GET /StallDetails2 - Returns a full list of customer stall assignments for a location. */
  @RequestMapping(value = "/StallDetails2", method = RequestMethod.GET, produces = "application/json")
  public ResponseEntity<Object> availableStallsinLocation2(@RequestParam(required = true) String locationId)
        throws NoContentException, IOException {
    logger.info("GetStallDetails::location:->{}", locationId);
    custBeanList = customerDataService.getRentalAppsData2(locationId);
    return new ResponseEntity<>(custBeanList, HttpStatus.OK);
  }
  
  /** DELETE /admin/dataretention - Deletes old customer data for a location based on time interval. */
  @RequestMapping(value = "/admin/dataretention", method = RequestMethod.DELETE, produces = "application/json")
  public ResponseEntity<Object> deleteOldData(@RequestParam(required = true) String locationId, 
        @RequestParam(required = true) String timeInterval)
        throws NoContentException, IOException {
    logger.info("DataRetention::location:->{}", locationId);
    try {
      custBeanList = customerDataService.deleteOldData(locationId, timeInterval);      
    } catch (ServiceException e) {
      //e.printStackTrace();
      logger.error("Exception caught:{}", e.getErrorMessage());
      return new ResponseEntity<>(e.getErrorMessage(), HttpStatus.UNPROCESSABLE_ENTITY);
    } catch (Exception e) {
      //e.printStackTrace();
      logger.error("Exception caught:{}. Please check the resource path is correct "
            + "and the entries in the resource are comma separated and try again.", e.getMessage());
      return new ResponseEntity<>(e.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY);
    }
   
    return new ResponseEntity<>(custBeanList, HttpStatus.OK);
  }
  
  /** POST /AdhocChanges - Processes ad-hoc customer data changes (add/update/delete) from the admin UI. */
  @RequestMapping(value = "/AdhocChanges", method = RequestMethod.POST, consumes = "application/json")
  public ResponseEntity<Object> adhocChanges(@RequestBody Rental rentalBean,
                                             @RequestParam(required = true, value = "locationId") String locationId)
        throws IOException, NoContentException, IllegalArgumentException {
    logger.info("PostAdhocChanges::location:->{}", locationId);
    if (rentalBean.getAction() == null) {
      throw new NoContentException("No changes to update");
    }
    logger.info("PostAdhocChanges::Rental changes received:->{}", rentalBean);
    CwaMessageBean cwaMessageBean = new CwaMessageBean();
    cwaMessageBean.setRental(rentalBean);
    try {
      customerDataService.persistData(locationId, cwaMessageBean);
    } catch (ApplicationException e) {
      return new ResponseEntity<>(e.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY);
    }
    return new ResponseEntity<>("Successfully updated posted changes", HttpStatus.OK);
  
  }

  /** POST /admin/ai/customer-query - Answers a natural-language question over one location's customer data. */
  @RequestMapping(value = "/admin/ai/customer-query", method = RequestMethod.POST,
      consumes = "application/json", produces = "application/json")
  public ResponseEntity<Object> queryCustomerData(@RequestBody AiCustomerQueryRequest request)
      throws IOException {
    try {
      AiCustomerQueryResponse response =
          customerAiQueryService.query(request.getLocationId(), request.getQuestion());
      return new ResponseEntity<>(response, HttpStatus.OK);
    } catch (IllegalArgumentException e) {
      return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
    } catch (IllegalStateException e) {
      return new ResponseEntity<>(e.getMessage(), HttpStatus.SERVICE_UNAVAILABLE);
    }
  }
  
  /** POST /admin/locations - Adds a new location to the location table. */
  @RequestMapping(value = "/admin/locations", method = RequestMethod.POST, produces = "application/json")
  public ResponseEntity<SuccessRespObj> addLocation(@RequestBody GbLocationReqObj inputLocation) 
      throws DatabaseException {

    logger.info("Start of com.rentalapps.controller.addLocation () ...");
    
    List<GbLocationRespObj> data = new ArrayList<>();
    data.add(customerDataService.addLocation(inputLocation));
    SuccessRespObj resp = new SuccessRespObj();
    resp.setData(data);
    resp.setMessage(DatabaseConstants.ADD_LOCATION_SUCCESS_MESSAGE);
    
    logger.info("End of com.rentalapps.controller.addLocation () ...");
    return new ResponseEntity<SuccessRespObj>(resp, HttpStatus.OK);
  }  

  /** GET /admin/locations - Retrieves all configured locations. */
  @RequestMapping(value = "/admin/locations", method = RequestMethod.GET, produces = "application/json")
  public ResponseEntity<SuccessRespObj> getLocations()
      throws DatabaseException, ServiceException {

    logger.info("Start of com.rentalapps.controller.getLocations () ...");

    List<GbLocationRespObj> data = customerDataService.getLocations();
    SuccessRespObj resp = new SuccessRespObj();
    resp.setData(data);
    resp.setMessage(DatabaseConstants.GET_LOCATION_SUCCESS_MESSAGE);

    logger.info("End of com.rentalapps.controller.getLocations () ...");
    return new ResponseEntity<SuccessRespObj>(resp, HttpStatus.OK);
  }

  /** PUT /admin/locations - Updates an existing location record. */
  @RequestMapping(value = "/admin/locations", method = RequestMethod.PUT, produces = "application/json")
  public ResponseEntity<SuccessRespObj> updateLocation(@RequestBody GbLocationReqObj inputLocation) 
      throws DatabaseException {

    logger.info("Start of com.rentalapps.controller.updateLocation () ...");

    List<GbLocationRespObj> data = new ArrayList<>();
    data.add(customerDataService.updateLocation(inputLocation));
    SuccessRespObj resp = new SuccessRespObj();
    resp.setData(data);
    resp.setMessage(DatabaseConstants.UPDATE_LOCATION_SUCCESS_MESSAGE);
    
    logger.info("End of com.rentalapps.controller.updateLocation () ...");
    return new ResponseEntity<SuccessRespObj>(resp, HttpStatus.OK);
  }  

  /** DELETE /admin/locations - Removes a location record. */
  @RequestMapping(value = "/admin/locations", method = RequestMethod.DELETE, produces = "application/json")
  public ResponseEntity<Object> removeLocation(@RequestBody GbLocationReqObj inputLocation) throws DatabaseException {

    logger.info("Start of com.rentalapps.controller.removeLocation () ...");

    List<GbLocationRespObj> data = new ArrayList<>();
    data.add(customerDataService.removeLocation(inputLocation));
    SuccessRespObj resp = new SuccessRespObj();
    resp.setData(data);
    resp.setMessage(DatabaseConstants.DELETE_LOCATION_SUCCESS_MESSAGE);
    
    logger.info("End of com.rentalapps.controller.removeLocation () ...");
    return new ResponseEntity<>(resp, HttpStatus.OK);
  }  

  /** GET /admin/locations/{id} - Retrieves a single location by its code. */
  @RequestMapping(value = "/admin/locations/{id}", method = RequestMethod.GET, produces = "application/json")
  public ResponseEntity<SuccessRespObj> getLocation(@PathVariable String id) 
      throws DatabaseException, ServiceException {
    
    logger.info("Start of com.rentalapps.controller.getLocation () ...");
    
    logger.info("id = ", id);
    List<GbLocationRespObj> data = customerDataService.getLocation(id);
    SuccessRespObj resp = new SuccessRespObj();
    resp.setData(data);
    resp.setMessage(DatabaseConstants.GET_LOCATION_SUCCESS_MESSAGE);
    
    logger.info("End of com.rentalapps.controller.getLocation () ...");
    return new ResponseEntity<SuccessRespObj>(resp, HttpStatus.OK);
  }  

  /** GET /admin/locations/list/{ids} - Retrieves multiple locations by comma-separated codes. */
  @RequestMapping(value = "/admin/locations/list/{ids}", method = RequestMethod.GET, produces = "application/json")
  public ResponseEntity<SuccessRespObj> getLocationList(@PathVariable List<String> ids) 
      throws DatabaseException, ServiceException {
    
    logger.info("Start of com.rentalapps.controller.getLocationList () ...");
    
    logger.info("ids = ", ids.stream().collect(Collectors.joining(" ")));
    List<GbLocationRespObj> data = customerDataService.getLocationList(ids);
    SuccessRespObj resp = new SuccessRespObj();
    resp.setData(data);
    resp.setMessage(DatabaseConstants.GET_LOCATION_SUCCESS_MESSAGE);
    
    logger.info("End of com.rentalapps.controller.getLocationList () ...");
    return new ResponseEntity<SuccessRespObj>(resp, HttpStatus.OK);
  }  

  /** GET /admin/locations/{id}/correlation-from-location - Returns the hex-encoded correlation ID for a location. */
  @RequestMapping(value = "/admin/locations/{id}/correlation-from-location", 
      method = RequestMethod.GET, produces = "application/json")
  public ResponseEntity<Object> getCorrelationId(@PathVariable String id)
      throws NoContentException, IOException {
    logger.info("getCorrelationId::location:->{}", id);
    CorrelationBean bean = customerDataService.getCorrelationId(id);
    if (bean == null) {
      throw new NoContentException();
    }
    return new ResponseEntity<>(bean, HttpStatus.OK);
  }  
  
  /** GET /admin/locations/{id}/location-from-correlation - Decodes a correlation ID back to a location code. */
  @RequestMapping(value = "/admin/locations/{id}/location-from-correlation", 
      method = RequestMethod.GET, produces = "application/json")
  public ResponseEntity<Object> getLocationId(@PathVariable String id)
      throws NoContentException, IOException {
    logger.info("getLocationId::location:->{}", id);
    CorrelationBean bean = customerDataService.getLocationFromCorrelationId(id);
    if (bean == null) {
      throw new NoContentException();
    }
    return new ResponseEntity<>(bean, HttpStatus.OK);
  }  

  /** GET /admin/time/{continent}/{city}/localtime-from-location - Returns local time info for a timezone. */
  @RequestMapping(value = "/admin/time/{continent}/{city}/localtime-from-location", 
      method = RequestMethod.GET, produces = "application/json")
  public ResponseEntity<Object> getLocalTime(@PathVariable String continent, @PathVariable String city)
      throws ServiceException {
    LocaltimeBean bean = customerDataService.getLocalTime(continent, city);
    return new ResponseEntity<>(bean, HttpStatus.OK);
  }  
 
}
