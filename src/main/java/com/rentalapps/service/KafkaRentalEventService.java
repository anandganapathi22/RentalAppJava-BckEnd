package com.rentalapps.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.rentalapps.exception.ApplicationException;
import com.rentalapps.model.MessageBean;
import com.rentalapps.model.Rental;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Converts Kafka rental events into the existing CWA message shape and persists them.
 */
@Service
public class KafkaRentalEventService {

  private static final Logger LOGGER = LoggerFactory.getLogger(KafkaRentalEventService.class);

  private final ObjectMapper objectMapper = new ObjectMapper()
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  private final XmlMapper xmlMapper = new XmlMapper();
  private final CustomerDataService customerDataService;

  public KafkaRentalEventService(CustomerDataService customerDataService) {
    this.customerDataService = customerDataService;
  }

  public void process(String topic, String key, Headers headers, String payload)
      throws IOException, ApplicationException {
    process(topic, key, headerValue(headers, "locationCode"), payload);
  }

  public void process(String source, String key, String locationCodeHeader, String payload)
      throws IOException, ApplicationException {
    if (StringUtils.isBlank(payload)) {
      LOGGER.warn("Skipping blank rental event payload from {}", source);
      return;
    }

    MessageBean messageBean = toMessageBean(payload);
    String locationCode = resolveLocationCode(messageBean, key, locationCodeHeader);
    if (StringUtils.isBlank(locationCode)) {
      throw new IllegalArgumentException("Rental event must include locationCode, key, or locationCode header");
    }

    LOGGER.info("Persisting rental event from {} for location {}", source, locationCode);
    customerDataService.persistQueueData(locationCode, messageBean);
  }

  private MessageBean toMessageBean(String payload) throws IOException {
    String trimmedPayload = payload.trim();
    if (trimmedPayload.startsWith("<")) {
      return xmlMapper.readValue(trimmedPayload, MessageBean.class);
    }

    JsonNode rootNode = objectMapper.readTree(trimmedPayload);
    MessageBean messageBean = new MessageBean();
    JsonNode rentalNode = rootNode.has("rental") ? rootNode.get("rental") : rootNode;

    if (rentalNode.isArray()) {
      for (JsonNode item : rentalNode) {
        messageBean.setRental(objectMapper.treeToValue(item, Rental.class));
      }
    } else {
      messageBean.setRental(objectMapper.treeToValue(rentalNode, Rental.class));
    }

    return messageBean;
  }

  private String resolveLocationCode(MessageBean messageBean, String key, String headerLocationCode) {
    return StringUtils.upperCase(StringUtils.trimToNull(
        firstRentalLocation(messageBean, key, headerLocationCode)));
  }

  private String firstRentalLocation(MessageBean messageBean, String key, String headerLocationCode) {
    if (messageBean != null && !messageBean.rental().isEmpty()) {
      String rentalLocationCode = messageBean.rental().get(0).getLocationCode();
      if (StringUtils.isNotBlank(rentalLocationCode)) {
        return rentalLocationCode;
      }
    }
    if (StringUtils.isNotBlank(headerLocationCode)) {
      return headerLocationCode;
    }
    return key;
  }

  private String headerValue(Headers headers, String name) {
    if (headers == null) {
      return null;
    }
    Header header = headers.lastHeader(name);
    if (header == null || header.value() == null) {
      return null;
    }
    return new String(header.value(), StandardCharsets.UTF_8);
  }
}
