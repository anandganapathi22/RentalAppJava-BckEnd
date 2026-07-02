package com.rentalapps.configmq;

import com.ibm.mq.jakarta.jms.MQConnectionFactory;
import jakarta.jms.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;


/**
 * IBM MQ connection configuration for primary and secondary queue managers.
 * Sets up JMS listener container factories and a Jackson message converter.
 */
@Configuration
@ConditionalOnProperty(name = "goldSign.MQ.enabled", havingValue = "true", matchIfMissing = true)
//@PropertySource(ignoreResourceNotFound = true, value = "classpath:application.yml")
public class ConnectionConfiguration {

  private static final Logger logger = LoggerFactory.getLogger(ConnectionConfiguration.class);

  @Value("${goldSign.MQ.host.primary}")
  private String primaryhost;

  @Value("${goldSign.MQ.port.primary}")
  private Integer primaryport;

  @Value("${goldSign.MQ.queue.manager.primary}")
  private String primaryqueueManager;

  @Value("${goldSign.MQ.channel.primary}")
  private String primarychannel;

  @Value("${goldSign.MQ.request.queue.primary}")
  private String primaryqueue;

  @Value("${goldSign.MQ.host.secondary}")
  private String secondaryhost;

  @Value("${goldSign.MQ.port.secondary}")
  private Integer secondaryport;

  @Value("${goldSign.MQ.queue.manager.secondary}")
  private String secondaryqueueManager;

  @Value("${goldSign.MQ.channel.secondary}")
  private String secondarychannel;

  @Value("${goldSign.MQ.request.queue.secondary}")
  private String secondaryqueue;

  /** Creates a JMS ConnectionFactory for the primary MQ host. */
  @Bean
  public ConnectionFactory connectionFactoryPrimary() {
    MQConnectionFactory factory = new MQConnectionFactory();
    try {
      factory.setHostName(primaryhost);
      factory.setPort(primaryport);
      factory.setQueueManager(primaryqueueManager);
      factory.setChannel(primarychannel);
      factory.setTransportType(com.ibm.msg.client.jakarta.wmq.WMQConstants.WMQ_CM_CLIENT);
      factory.setUseConnectionPooling(true);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return factory;
  }


  /** Creates a JMS listener container factory using the primary connection. */
  @Bean
  public DefaultJmsListenerContainerFactory jmsListenerContainerFactoryPrimary() {
    DefaultJmsListenerContainerFactory factory
          = new DefaultJmsListenerContainerFactory();
    factory.setConnectionFactory(connectionFactoryPrimary());
    return factory;
  }

  /** Creates a JMS ConnectionFactory for the secondary (failover) MQ host. */
  @Bean
  public ConnectionFactory connectionFactorySecondary() {
    MQConnectionFactory factory = new MQConnectionFactory();
    try {
      factory.setHostName(secondaryhost);
      factory.setPort(secondaryport);
      factory.setQueueManager(secondaryqueueManager);
      factory.setChannel(secondarychannel);
      factory.setTransportType(com.ibm.msg.client.jakarta.wmq.WMQConstants.WMQ_CM_CLIENT);
      factory.setUseConnectionPooling(true);      
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return factory;
  }


  /** Creates a JMS listener container factory using the secondary connection. */
  @Bean
  public DefaultJmsListenerContainerFactory jmsListenerContainerFactorySecondary() {
    DefaultJmsListenerContainerFactory factory
          = new DefaultJmsListenerContainerFactory();
    factory.setConnectionFactory(connectionFactorySecondary());
    return factory;
  }

  /** Provides a Jackson-based JMS message converter for JSON text messages. */
  @Bean
  public MessageConverter jacksonJmsMessageConverter() {
    MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
    converter.setTargetType(MessageType.TEXT);
    converter.setTypeIdPropertyName("_type");
    return converter;
  }
}
