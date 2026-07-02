package com.rentalapps.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentalapps.model.Event;
import com.rentalapps.model.EventType;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Local event implementation used when the application persists directly to H2.
 */
@Service
public class EventServiceImpl<T> implements EventService<T> {

  private static final Logger log = LoggerFactory.getLogger(EventServiceImpl.class);
  private static final String SERVICE_NAME = "RENTAL_APPS";

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public void send(final EventType eventType, final T message) {
    var correlationId = UUID.randomUUID();
    var event = new Event<T>();

    event.setServiceName(SERVICE_NAME);
    event.setEventType(eventType.name());
    event.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
    event.setCorrelationId(correlationId);
    event.setMessage(message);

    try {
      log.info("Rental Apps event: {}", objectMapper.writeValueAsString(event));
    } catch (JsonProcessingException exception) {
      log.error("Error serializing event", exception);
    }
  }
}
