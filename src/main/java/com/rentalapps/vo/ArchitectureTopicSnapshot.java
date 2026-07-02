package com.rentalapps.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/** Snapshot of a local Kafka-style topic. */
@Data
@AllArgsConstructor
public class ArchitectureTopicSnapshot {
  private String name;
  private String description;
  private long eventCount;
}
