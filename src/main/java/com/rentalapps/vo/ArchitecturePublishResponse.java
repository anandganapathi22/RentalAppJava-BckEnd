package com.rentalapps.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/** Response returned after publishing an event through the architecture simulator. */
@Data
@AllArgsConstructor
public class ArchitecturePublishResponse {
  private String message;
  private String inboundTopic;
  private String displayTopic;
  private String auditTopic;
}
