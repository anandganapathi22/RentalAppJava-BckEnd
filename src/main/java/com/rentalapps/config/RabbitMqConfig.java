package com.rentalapps.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** RabbitMQ queue, exchange, binding, and listener configuration for local rental events. */
@Configuration
@ConditionalOnProperty(name = "rental.rabbitmq.enabled", havingValue = "true")
public class RabbitMqConfig {

  @Value("${rental.rabbitmq.queue}")
  private String queueName;

  @Value("${rental.rabbitmq.exchange}")
  private String exchangeName;

  @Value("${rental.rabbitmq.routing-key}")
  private String routingKey;

  @Bean
  public Queue rentalRabbitQueue() {
    return new Queue(queueName, true);
  }

  @Bean
  public DirectExchange rentalRabbitExchange() {
    return new DirectExchange(exchangeName, true, false);
  }

  @Bean
  public Binding rentalRabbitBinding(Queue rentalRabbitQueue, DirectExchange rentalRabbitExchange) {
    return BindingBuilder.bind(rentalRabbitQueue).to(rentalRabbitExchange).with(routingKey);
  }

  @Bean
  public SimpleRabbitListenerContainerFactory rentalRabbitListenerContainerFactory(
      ConnectionFactory connectionFactory) {
    SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
    factory.setConnectionFactory(connectionFactory);
    return factory;
  }
}
