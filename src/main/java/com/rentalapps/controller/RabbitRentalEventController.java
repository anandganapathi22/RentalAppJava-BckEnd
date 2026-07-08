package com.rentalapps.controller;

import com.rentalapps.service.RabbitRentalEventPublisher;
import com.rentalapps.vo.ArchitectureEventRequest;
import com.rentalapps.vo.RabbitPublishResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST endpoint for publishing local rental events to RabbitMQ. */
@RestController
@RequestMapping("/api/rabbitmq")
@ConditionalOnProperty(name = "rental.rabbitmq.enabled", havingValue = "true")
public class RabbitRentalEventController {

  private final RabbitRentalEventPublisher publisher;
  private final String exchange;
  private final String routingKey;
  private final String queue;

  public RabbitRentalEventController(RabbitRentalEventPublisher publisher,
                                     @Value("${rental.rabbitmq.exchange}") String exchange,
                                     @Value("${rental.rabbitmq.routing-key}") String routingKey,
                                     @Value("${rental.rabbitmq.queue}") String queue) {
    this.publisher = publisher;
    this.exchange = exchange;
    this.routingKey = routingKey;
    this.queue = queue;
  }

  @PostMapping("/events")
  public ResponseEntity<RabbitPublishResponse> publish(@RequestBody ArchitectureEventRequest request)
      throws IOException {
    publisher.publish(request);
    return ResponseEntity.ok(new RabbitPublishResponse(
        "Event enqueued to RabbitMQ",
        exchange,
        routingKey,
        queue));
  }
}
