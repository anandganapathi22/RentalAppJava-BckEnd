package com.rentalapps.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/** Audit event projection from the customer audit table. */
@Data
@AllArgsConstructor
public class ArchitectureAuditRecord {
  private String id;
  private String operationTime;
  private String operationDate;
  private String operation;
  private String sourceSystem;
  private String locationCode;
  private String customerName;
  private String stall;
  private String ra;
}
