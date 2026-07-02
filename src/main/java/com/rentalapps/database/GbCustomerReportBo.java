package com.rentalapps.database;

import lombok.Data;

/**
 * Report business object used during the scheduled deletion job to track
 * each customer record's deletion status and reason.
 */
@Data
public class GbCustomerReportBo {

  private String id;
  private String customerName;
  private String locationCode;
  private String stall;
  private String oneClub;
  private String ra;
  private String arrivalDate;
  private String arrivalTime;
  private String status;
  private String reason;
}
