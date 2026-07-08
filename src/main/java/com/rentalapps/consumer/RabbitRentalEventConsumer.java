package com.rentalapps.consumer;

import com.rentalapps.service.KafkaRentalEventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/** RabbitMQ consumer for local rental events. */
@Component
@ConditionalOnProperty(name = "rental.rabbitmq.enabled", havingValue = "true")
public class RabbitRentalEventConsumer {

  private static final Logger LOGGER = LoggerFactory.getLogger(RabbitRentalEventConsumer.class);

  private final KafkaRentalEventService rentalEventService;

  public RabbitRentalEventConsumer(KafkaRentalEventService rentalEventService) {
    this.rentalEventService = rentalEventService;
  }

  @RabbitListener(
      queues = "${rental.rabbitmq.queue}",
      containerFactory = "rentalRabbitListenerContainerFactory")
  public void receive(String payload,
                      @Header(name = "locationCode", required = false) String locationCode,
                      @Header(name = AmqpHeaders.RECEIVED_ROUTING_KEY, required = false) String routingKey) {
    try {
      rentalEventService.process("rabbitmq:" + routingKey, null, locationCode, payload);
    } catch (Exception e) {
      LOGGER.error("Failed to process RabbitMQ rental event from routing key {}", routingKey, e);
      throw new IllegalStateException("Failed to process RabbitMQ rental event", e);
    }
  }
}
