package com.rentalapps.model;


import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

//@JsonIgnoreProperties({"action"}) 
/**
 * Bean representing a rental record from a CWA XML message.
 * Maps XML elements (action, ra, customer, stall, arrival-date, arrival-time, etc.) to fields.
 */
@Data
public class Rental {
  private String action;
  private String ra;
  @JacksonXmlProperty(localName = "oneclub")
  private String oneClub;
  @JacksonXmlProperty(localName = "customer")
  private String customerName;
  private String stall;
  @JacksonXmlProperty(localName = "arrival-date")
  private String arrivalDate;
  @JacksonXmlProperty(localName = "arrival-time")
  private String arrivalTime;
  private String vip;
  private String id;
  private String locationCode;
  private String createdDateTime;
  private String updatedDateTime;
}
