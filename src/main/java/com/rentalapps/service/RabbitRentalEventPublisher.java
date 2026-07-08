package com.rentalapps.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentalapps.vo.ArchitectureEventRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/** Publishes local rental event payloads to RabbitMQ. */
@Service
@ConditionalOnProperty(name = "rental.rabbitmq.enabled", havingValue = "true")
public class RabbitRentalEventPublisher {

  private final RabbitTemplate rabbitTemplate;
  private final ObjectMapper objectMapper;
  private final String exchange;
  private final String routingKey;

  public RabbitRentalEventPublisher(RabbitTemplate rabbitTemplate,
                                    ObjectMapper objectMapper,
                                    @Value("${rental.rabbitmq.exchange}") String exchange,
                                    @Value("${rental.rabbitmq.routing-key}") String routingKey) {
    this.rabbitTemplate = rabbitTemplate;
    this.objectMapper = objectMapper;
    this.exchange = exchange;
    this.routingKey = routingKey;
  }

  public void publish(ArchitectureEventRequest request) throws IOException {
    String payload = objectMapper.writeValueAsString(request);
    Message message = MessageBuilder
        .withBody(payload.getBytes(StandardCharsets.UTF_8))
        .setContentType(MessageProperties.CONTENT_TYPE_TEXT_PLAIN)
        .setHeader("locationCode", StringUtils.trimToEmpty(request.getLocationCode()).toUpperCase())
        .build();
    rabbitTemplate.send(exchange, routingKey, message);
  }
}
