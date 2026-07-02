package com.rentalapps.consumer;

import com.rentalapps.service.KafkaRentalEventService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for rental events. This runs alongside the MQ consumer when enabled.
 */
@Component
@ConditionalOnProperty(name = "rental.kafka.enabled", havingValue = "true")
public class KafkaRentalEventConsumer {

  private static final Logger LOGGER = LoggerFactory.getLogger(KafkaRentalEventConsumer.class);

  private final KafkaRentalEventService kafkaRentalEventService;

  public KafkaRentalEventConsumer(KafkaRentalEventService kafkaRentalEventService) {
    this.kafkaRentalEventService = kafkaRentalEventService;
  }

  @KafkaListener(
      topics = {
          "${rental.kafka.topics.rental-us}",
          "${rental.kafka.topics.rental-eu}",
          "${rental.kafka.topics.adhoc}"
      },
      containerFactory = "rentalKafkaListenerContainerFactory")
  public void receive(ConsumerRecord<String, String> record) {
    try {
      kafkaRentalEventService.process(record.topic(), record.key(), record.headers(), record.value());
    } catch (Exception e) {
      LOGGER.error("Failed to process Kafka rental event from topic {} partition {} offset {}",
          record.topic(), record.partition(), record.offset(), e);
    }
  }
}
