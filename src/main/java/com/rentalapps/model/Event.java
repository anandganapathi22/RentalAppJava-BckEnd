package com.rentalapps.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;
import lombok.Data;

/**
 * Generic event wrapper for Rental Apps event logging.
 * Contains service metadata, event type, correlation ID, and the typed message payload.
 *
 * @param <T> the type of the event message payload
 */
@Data
public class Event<T> {

  @JsonProperty("ServiceName")
  private String serviceName;

  @JsonProperty("Timestamp")
  private String timestamp;

  @JsonProperty("EventType")
  private String eventType;

  @JsonProperty("CorrelationId")
  private UUID correlationId;

  @JsonProperty("Message")
  private T message;
}
