package com.rentalapps.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.rentalapps.exception.ApplicationException;
import com.rentalapps.model.MessageBean;
import com.rentalapps.service.CustomerDataService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

/**
 * JMS message consumer that listens to CWA queues (primary and secondary)
 * and processes incoming rental/customer XML messages.
 */
@Component
@ConditionalOnProperty(name = "goldSign.MQ.enabled", havingValue = "true", matchIfMissing = true)
public class QueueConsumer {
  Logger logger = LoggerFactory.getLogger(QueueConsumer.class);

  @Autowired
  public CustomerDataService customerDataService;

  /**
   * Receives and processes a JMS message from the CWA queue.
   * Decodes the correlation ID, extracts the XML payload, and persists the data.
   */
  @JmsListener(destination = "${goldSign.MQ.request.queue.primary}",
        containerFactory = "jmsListenerContainerFactoryPrimary",
        selector = "${goldSign.MQ.consumer.locationsFilter}")
  @JmsListener(destination = "${goldSign.MQ.request.queue.secondary}",
        containerFactory = "jmsListenerContainerFactorySecondary",
          selector = "${goldSign.MQ.consumer.locationsFilter}")
  public void receive(Message message) throws JSONException, IOException, DecoderException,
        IllegalArgumentException, ApplicationException {    
    logger.info(this.getClass().getSimpleName());
    String encodedCorrelationId = Arrays.asList(message.getHeaders().get("jms_correlationId").toString().split(":"))
          .get(1);
    logger.info("Received message corrId is: " + encodedCorrelationId);
    logger.info("Received message Body is: " + message.getPayload());
    String decodedCorrelationId = new String(Hex.decodeHex(encodedCorrelationId.toCharArray()),
          StandardCharsets.UTF_8).trim();
    logger.info("decodedCorrelationId::" + decodedCorrelationId);
    try {
      String originalPayload = message.getPayload().toString();
      String xmlPayload = "";
      if (StringUtils.isNotBlank(originalPayload)) {
        logger.info("Payload has content, Proceed...");
        int index = originalPayload.indexOf("<");
        xmlPayload = originalPayload.substring(index);
      } else {
        logger.info("Payload is empty. Exiting...");
        return;
      }
      logger.info("Extracted Payload is :: {}", xmlPayload);

      XmlMapper xmlMapper = new XmlMapper();
      MessageBean messageBean = xmlMapper.readValue(xmlPayload, MessageBean.class);
      logger.info("Value of messageBean->{}", messageBean);
      logger.info("Initiating store data to DB flow");
      customerDataService.persistQueueData(decodedCorrelationId, messageBean);
    } catch (JsonProcessingException e) {
      logger.error("JSON Processing Exception in QueueConsumer", e);
    }
  }
}
