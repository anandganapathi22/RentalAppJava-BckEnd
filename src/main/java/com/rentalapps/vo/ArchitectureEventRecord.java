package com.rentalapps.vo;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;

/** Recent event recorded by the local event backbone. */
@Data
@AllArgsConstructor
public class ArchitectureEventRecord {
  private Instant timestamp;
  private String topic;
  private String source;
  private String region;
  private String eventType;
  private String locationCode;
  private String customerName;
  private String message;
}
