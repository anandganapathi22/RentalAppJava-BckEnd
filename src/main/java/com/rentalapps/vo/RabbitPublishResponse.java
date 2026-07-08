package com.rentalapps.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/** Response returned after a rental event is enqueued to RabbitMQ. */
@Data
@AllArgsConstructor
public class RabbitPublishResponse {
  private String message;
  private String exchange;
  private String routingKey;
  private String queue;
}
