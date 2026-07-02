package com.rentalapps.database;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response object representing a customer record returned to API callers.
 * Includes an identifier field to distinguish stall vs zone assignments.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GbCustomerRespObj {

  private String id;
  private String customerName;
  private String locationCode;
  private String oneClub;
  private String ra;
  private String stall;
  private String arrivalDate;
  private String arrivalTime;
  private String createdDateTime;
  private String updatedDateTime;
  private String identifier;
}
