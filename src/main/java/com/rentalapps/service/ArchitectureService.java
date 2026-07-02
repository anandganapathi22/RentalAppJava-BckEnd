package com.rentalapps.service;

import com.rentalapps.config.ApplicationConfig;
import com.rentalapps.exception.ApplicationException;
import com.rentalapps.exception.DatabaseException;
import com.rentalapps.model.CwaMessageBean;
import com.rentalapps.model.Rental;
import com.rentalapps.vo.ArchitectureAuditRecord;
import com.rentalapps.vo.ArchitectureEventRecord;
import com.rentalapps.vo.ArchitectureEventRequest;
import com.rentalapps.vo.ArchitectureOverviewResponse;
import com.rentalapps.vo.ArchitecturePublishResponse;
import com.rentalapps.vo.ArchitectureTopicSnapshot;
import com.rentalapps.vo.CustomerBean;
import com.rentalapps.vo.GbCustomerRespObj;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** Local implementation of the Kafka-centered architecture shown in the UI diagram. */
@Service
public class ArchitectureService {

  private static final String TOPIC_RENTAL_EVENTS_US = "rental-events-us";
  private static final String TOPIC_RENTAL_EVENTS_EU = "rental-events-eu";
  private static final String TOPIC_ADHOC = "adhoc-goldboard-events";
  private static final String TOPIC_DISPLAY = "goldboard-display-events";
  private static final String TOPIC_AUDIT = "goldboard-audit-events";
  private static final String TOPIC_DEAD_LETTER = "dead-letter-events";
  private static final int RECENT_EVENT_LIMIT = 40;

  private final CustomerDataService customerDataService;
  private final DbService dbService;
  private final JdbcTemplate jdbcTemplate;
  private final ApplicationConfig appConfig;
  private final Map<String, AtomicLong> topicCounters = new ConcurrentHashMap<>();
  private final ArrayDeque<ArchitectureEventRecord> recentEvents = new ArrayDeque<>();

  public ArchitectureService(CustomerDataService customerDataService,
                             DbService dbService,
                             JdbcTemplate jdbcTemplate,
                             ApplicationConfig appConfig) {
    this.customerDataService = customerDataService;
    this.dbService = dbService;
    this.jdbcTemplate = jdbcTemplate;
    this.appConfig = appConfig;
    topicDescriptions().keySet().forEach(topic -> topicCounters.put(topic, new AtomicLong()));
  }

  public ArchitectureOverviewResponse overview() throws DatabaseException {
    long usCount = 0;
    long euCount = 0;
    for (GbCustomerRespObj customer : dbService.getCustomerList()) {
      if (dbService.isEuropeLocation(customer.getLocationCode())) {
        euCount++;
      } else {
        usCount++;
      }
    }
    return new ArchitectureOverviewResponse(
        topicSnapshots(),
        usCount,
        euCount,
        usCount + euCount,
        recentEvents()
    );
  }

  public ArchitecturePublishResponse publish(ArchitectureEventRequest request)
      throws IOException, ApplicationException {
    String region = normalizeRegion(request.getRegion(), request.getLocationCode());
    String inboundTopic = inboundTopic(region, request.getSource());
    String displayTopic = TOPIC_DISPLAY;
    String auditTopic = TOPIC_AUDIT;

    Rental rental = new Rental();
    rental.setAction(defaultValue(request.getAction(), "add"));
    rental.setLocationCode(StringUtils.trimToEmpty(request.getLocationCode()).toUpperCase(Locale.ROOT));
    rental.setCustomerName(defaultValue(request.getCustomerName(), "Sample Customer"));
    rental.setOneClub(defaultValue(request.getOneClub(), "OC" + System.currentTimeMillis()));
    rental.setRa(defaultValue(request.getRa(), "RA" + System.currentTimeMillis()));
    rental.setStall(defaultValue(request.getStall(), region.equals("EU") ? "ZONE 1" : "A12"));
    rental.setArrivalDate(defaultValue(request.getArrivalDate(), "07/02/2026"));
    rental.setArrivalTime(defaultValue(request.getArrivalTime(), "10:00"));

    CwaMessageBean message = new CwaMessageBean();
    message.setRental(rental);

    increment(inboundTopic, request, rental, "Inbound event accepted");
    try {
      customerDataService.persistData(rental.getLocationCode(), message);
      increment(displayTopic, request, rental, "Display projection updated");
      increment(auditTopic, request, rental, "Audit history recorded");
    } catch (RuntimeException | IOException exception) {
      increment(TOPIC_DEAD_LETTER, request, rental, exception.getMessage());
      throw exception;
    }

    return new ArchitecturePublishResponse(
        "Event published and processed",
        inboundTopic,
        displayTopic,
        auditTopic
    );
  }

  public List<CustomerBean> display(String region, String locationId) throws IOException, DatabaseException {
    String normalizedRegion = StringUtils.trimToEmpty(region).toUpperCase(Locale.ROOT);
    List<CustomerBean> customers = StringUtils.isBlank(locationId)
        ? allCustomers()
        : customerDataService.getRentalAppsData2(locationId);
    return customers.stream()
        .filter(customer -> {
          boolean europeLocation = dbService.isEuropeLocation(customer.getLocationCode());
          return "EU".equals(normalizedRegion) ? europeLocation : !europeLocation;
        })
        .toList();
  }

  public List<ArchitectureAuditRecord> auditEvents(int limit) {
    int boundedLimit = Math.max(1, Math.min(limit, 100));
    String sql = "select \"id\", \"operationTime\", \"operationDate\", \"OPERATION\", \"sourceSystem\", "
        + "\"locationCode\", \"customerName\", \"stall\", \"ra\" from " + quote(appConfig.getShadowTable())
        + " order by \"operationTime\" desc limit ?";
    return jdbcTemplate.query(sql, (rs, rowNum) -> new ArchitectureAuditRecord(
        rs.getString("id"),
        rs.getString("operationTime"),
        rs.getString("operationDate"),
        rs.getString("OPERATION"),
        rs.getString("sourceSystem"),
        rs.getString("locationCode"),
        rs.getString("customerName"),
        rs.getString("stall"),
        rs.getString("ra")
    ), boundedLimit);
  }

  private List<CustomerBean> allCustomers() throws DatabaseException {
    return dbService.getCustomerList().stream().map(customer -> {
      CustomerBean bean = new CustomerBean();
      bean.setId(customer.getId());
      bean.setCustomerName(customer.getCustomerName());
      bean.setLocationCode(customer.getLocationCode());
      bean.setStall(customer.getStall());
      bean.setOneClub(customer.getOneClub());
      bean.setRa(customer.getRa());
      bean.setArrivalDate(customer.getArrivalDate());
      bean.setArrivalTime(customer.getArrivalTime());
      bean.setCreatedDateTime(customer.getCreatedDateTime());
      bean.setUpdatedDateTime(customer.getUpdatedDateTime());
      bean.setIdentifier(customer.getIdentifier());
      return bean;
    }).toList();
  }

  private void increment(String topic, ArchitectureEventRequest request, Rental rental, String message) {
    topicCounters.computeIfAbsent(topic, key -> new AtomicLong()).incrementAndGet();
    ArchitectureEventRecord record = new ArchitectureEventRecord(
        Instant.now(),
        topic,
        defaultValue(request.getSource(), "UI"),
        normalizeRegion(request.getRegion(), rental.getLocationCode()),
        rental.getAction(),
        rental.getLocationCode(),
        rental.getCustomerName(),
        message
    );
    synchronized (recentEvents) {
      recentEvents.addFirst(record);
      while (recentEvents.size() > RECENT_EVENT_LIMIT) {
        recentEvents.removeLast();
      }
    }
  }

  private List<ArchitectureEventRecord> recentEvents() {
    synchronized (recentEvents) {
      return new ArrayList<>(recentEvents);
    }
  }

  private List<ArchitectureTopicSnapshot> topicSnapshots() {
    return topicDescriptions().entrySet().stream()
        .map(entry -> new ArchitectureTopicSnapshot(
            entry.getKey(),
            entry.getValue(),
            topicCounters.computeIfAbsent(entry.getKey(), key -> new AtomicLong()).get()
        ))
        .toList();
  }

  private Map<String, String> topicDescriptions() {
    Map<String, String> descriptions = new LinkedHashMap<>();
    descriptions.put(TOPIC_RENTAL_EVENTS_US, "US rental events from upstream producers");
    descriptions.put(TOPIC_RENTAL_EVENTS_EU, "EU rental events from upstream producers");
    descriptions.put(TOPIC_ADHOC, "UI/admin ad-hoc changes");
    descriptions.put(TOPIC_DISPLAY, "Display payload update events");
    descriptions.put(TOPIC_AUDIT, "Audit and history events");
    descriptions.put(TOPIC_DEAD_LETTER, "Failed event routing");
    return descriptions;
  }

  private String inboundTopic(String region, String source) {
    if ("UI".equalsIgnoreCase(StringUtils.trimToEmpty(source))
        || "ADMIN".equalsIgnoreCase(StringUtils.trimToEmpty(source))) {
      return TOPIC_ADHOC;
    }
    return "EU".equals(region) ? TOPIC_RENTAL_EVENTS_EU : TOPIC_RENTAL_EVENTS_US;
  }

  private String normalizeRegion(String requestedRegion, String locationCode) {
    String region = StringUtils.trimToEmpty(requestedRegion).toUpperCase(Locale.ROOT);
    if ("US".equals(region) || "EU".equals(region)) {
      return region;
    }
    return dbService.isEuropeLocation(locationCode) ? "EU" : "US";
  }

  private String defaultValue(String value, String fallback) {
    return StringUtils.defaultIfBlank(value, fallback);
  }

  private String quote(String identifier) {
    return "\"" + StringUtils.defaultString(identifier).replace("\"", "\"\"") + "\"";
  }
}
