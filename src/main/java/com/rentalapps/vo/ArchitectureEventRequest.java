package com.rentalapps.vo;

import lombok.Data;

/** Request for publishing a rental event through the local architecture simulator. */
@Data
public class ArchitectureEventRequest {
  private String source;
  private String region;
  private String action;
  private String locationCode;
  private String customerName;
  private String oneClub;
  private String ra;
  private String stall;
  private String arrivalDate;
  private String arrivalTime;
}
