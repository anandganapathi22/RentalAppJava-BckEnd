package com.rentalapps.service;

import com.rentalapps.model.EventType;

/**
 * Interface for publishing Rental Apps events.
 *
 * @param <T> the type of the event message payload
 */
public interface EventService<T> {
  /** Sends an event of the given type with the specified message payload. */
  void send(final EventType eventType, final T message);
}
