package com.rentalapps.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentalapps.vo.GbLocationRespObj;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class H2SampleDataSeeder {

  private static final Logger log = LoggerFactory.getLogger(H2SampleDataSeeder.class);
  private static final String LOCATION_SEED_PATH = "seed/locations.json";
  private static final int CUSTOMER_COUNT = 10000;
  private static final String[] SOURCES = {"DASH", "MQ"};
  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
  private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
  private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Bean
  public ApplicationRunner seedH2SampleData(JdbcTemplate jdbcTemplate, ApplicationConfig appConfig) {
    return args -> {
      String locationTable = quoted(appConfig.getLocationTable());
      String customerTable = quoted(appConfig.getDatabaseTable());
      List<GbLocationRespObj> seedLocations = loadSeedLocations();

      Integer locationRows = jdbcTemplate.queryForObject("select count(*) from " + locationTable, Integer.class);
      Integer customerRows = jdbcTemplate.queryForObject("select count(*) from " + customerTable, Integer.class);

      if (locationRows != null && locationRows == 0) {
        seedLocations(jdbcTemplate, locationTable, seedLocations);
      } else {
        log.info("Skipping location seed. Existing rows={}", locationRows);
      }

      if (customerRows != null && customerRows == 0) {
        seedCustomers(jdbcTemplate, customerTable, seedLocations);
      } else {
        log.info("Skipping customer seed. Existing rows={}", customerRows);
      }
    };
  }

  private List<GbLocationRespObj> loadSeedLocations() throws Exception {
    ClassPathResource resource = new ClassPathResource(LOCATION_SEED_PATH);
    try (InputStream inputStream = resource.getInputStream()) {
      List<GbLocationRespObj> locations =
          objectMapper.readValue(inputStream, new TypeReference<List<GbLocationRespObj>>() {
          });
      if (locations.isEmpty()) {
        throw new IllegalStateException("Location seed file is empty: " + LOCATION_SEED_PATH);
      }
      return locations;
    }
  }

  private void seedLocations(JdbcTemplate jdbcTemplate, String locationTable,
                             List<GbLocationRespObj> locations) {
    String sql = "insert into " + locationTable
        + " (\"hertzLocationCode\", \"displayName\", \"timeZone\") values (?, ?, ?)";

    List<Object[]> batchArgs = new ArrayList<>(locations.size());
    for (GbLocationRespObj location : locations) {
      batchArgs.add(new Object[] {
          location.getHertzLocationCode(),
          location.getDisplayName(),
          location.getTimeZone()
      });
    }

    jdbcTemplate.batchUpdate(sql, batchArgs);
    log.info("Seeded {} locations into {}", locations.size(), locationTable);
  }

  private void seedCustomers(JdbcTemplate jdbcTemplate, String customerTable,
                             List<GbLocationRespObj> locations) {
    String sql = "insert into " + customerTable
        + " (\"id\", \"customerName\", \"locationCode\", \"stall\", \"oneClub\", \"ra\","
        + " \"sourceSystem\", \"arrivalDate\", \"arrivalTime\", \"createdDatetime\", \"updatedDatetime\")"
        + " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    List<Object[]> batchArgs = new ArrayList<>(CUSTOMER_COUNT);
    LocalDateTime now = LocalDateTime.now();

    for (int i = 1; i <= CUSTOMER_COUNT; i++) {
      LocalDateTime arrival = now.minusMinutes(i);
      String locationCode = locations.get((i - 1) % locations.size()).getHertzLocationCode();
      String customerId = String.format(Locale.ROOT, "CUST%05d", i);
      String customerName = String.format(Locale.ROOT, "Customer %04d", i);
      String stall = String.format(Locale.ROOT, "S-%03d", ((i - 1) % 500) + 1);
      String oneClub = String.format(Locale.ROOT, "OC%08d", 10000000 + i);
      String ra = String.format(Locale.ROOT, "RA%07d", 2000000 + i);
      String sourceSystem = SOURCES[i % SOURCES.length];

      batchArgs.add(new Object[] {
          customerId,
          customerName,
          locationCode,
          stall,
          oneClub,
          ra,
          sourceSystem,
          arrival.toLocalDate().format(DATE_FORMAT),
          arrival.toLocalTime().format(TIME_FORMAT),
          now.minusDays(ThreadLocalRandom.current().nextInt(1, 15)).format(DATE_TIME_FORMAT),
          now.minusHours(ThreadLocalRandom.current().nextInt(0, 12)).format(DATE_TIME_FORMAT)
      });
    }

    jdbcTemplate.batchUpdate(sql, batchArgs);
    log.info("Seeded {} customers into {}", CUSTOMER_COUNT, customerTable);
  }

  private String quoted(String identifier) {
    return "\"" + identifier.replace("\"", "\"\"") + "\"";
  }
}
