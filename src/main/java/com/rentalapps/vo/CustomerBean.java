package com.rentalapps.vo;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Full customer data bean used in API responses, including all fields
 * such as timestamps and stall/zone identifier.
 */
@Data
@NoArgsConstructor
public class CustomerBean {

  private String customerName;
  private String stall;
  private String id;
  private String locationCode;
  private String oneClub;
  private String ra;
  private String arrivalDate;
  private String arrivalTime;
  private String createdDateTime;
  private String updatedDateTime;
  private String identifier;

  public CustomerBean(String customerName, String stall) {
    super();
    this.customerName = customerName;
    this.stall = stall;
  }
}
