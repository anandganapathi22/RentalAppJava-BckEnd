package com.rentalapps.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request object for customer CRUD operations, carrying input data from API or MQ consumers.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GbCustomerReqObj {

  private String id;
  private String customerName;
  private String locationCode;
  private String oneClub;
  private String ra;
  private String stall;
  private String arrivalDate;
  private String arrivalTime;
}
