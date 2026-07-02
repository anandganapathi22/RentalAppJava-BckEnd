package com.rentalapps.vo;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

/** High-level architecture dashboard response. */
@Data
@AllArgsConstructor
public class ArchitectureOverviewResponse {
  private List<ArchitectureTopicSnapshot> topics;
  private long usCustomerCount;
  private long euCustomerCount;
  private long totalCustomerCount;
  private List<ArchitectureEventRecord> recentEvents;
}
