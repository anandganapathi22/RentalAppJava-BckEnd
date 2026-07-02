package com.rentalapps.vo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Data;

/**
 * Business object representing a Rental Apps customer record retrieved from the database.
 * Supports construction from JSON via Gson deserialization.
 */
@Data
public class GbCustomerBo {

  private String id;
  private String customerName;
  private String locationCode;
  private String stall;
  private String oneClub;
  private String ra;
  private String arrivalDate;
  private String arrivalTime;
  private String createdDatetime;
  private String updatedDatetime;

  
  public GbCustomerBo(String id, String customerName, String locationCode, String stall, String oneClub,
                      String ra, String arrivalDate, String arrivalTime, String createdDatetime,
                      String updatedDatetime) {
    this.id = id;
    this.customerName = customerName;
    this.locationCode = locationCode;
    this.stall = stall;
    this.oneClub = oneClub;
    this.ra = ra;
    this.arrivalDate = arrivalDate;
    this.arrivalTime = arrivalTime;
    this.createdDatetime = createdDatetime;
    this.updatedDatetime = updatedDatetime;
  }

  public GbCustomerBo(String json) {

    Gson gson = new Gson();
    GbCustomerBo tempCustomer = gson.fromJson(json, GbCustomerBo.class);
    this.id = tempCustomer.id;
    this.customerName = tempCustomer.customerName;
    this.locationCode = tempCustomer.locationCode;
    this.stall = tempCustomer.stall;
    this.oneClub = tempCustomer.oneClub;
    this.ra = tempCustomer.ra;
    this.arrivalDate = tempCustomer.arrivalDate;
    this.arrivalTime = tempCustomer.arrivalTime;
    this.createdDatetime = tempCustomer.createdDatetime;
    this.updatedDatetime = tempCustomer.updatedDatetime;
  }

  @Override
  public String toString() {

    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    return gson.toJson(this);
  }

  public String getra() {
    return ra;
  }

  public void setra(String ra) {
    this.ra = ra;
  }
}
