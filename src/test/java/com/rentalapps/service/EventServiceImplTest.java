package com.rentalapps.service;

import com.rentalapps.model.EventType;
import org.junit.jupiter.api.Test;

class EventServiceImplTest {

  @Test
  void sendLogsLocalEvent() {
    EventService<String> eventService = new EventServiceImpl<>();
    eventService.send(EventType.RENTAL_APPS_ADD_EVENT, "message");
  }
}
