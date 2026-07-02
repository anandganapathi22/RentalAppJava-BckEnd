package com.rentalapps.config;

import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class H2SampleDataSeeder {

  private static final Logger log = LoggerFactory.getLogger(H2SampleDataSeeder.class);
  private static final int LOCATION_COUNT = 200;
  private static final int CUSTOMER_COUNT = 1000;
  private static final String[] TIME_ZONES = {
      "America/Chicago",
      "America/New_York",
      "America/Los_Angeles",
      "America/Denver",
      "Europe/London"
  };
  private static final String[] SOURCES = {"DASH", "MQ"};
  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
  private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
  private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  @Bean
  public ApplicationRunner seedH2SampleData(JdbcTemplate jdbcTemplate, ApplicationConfig appConfig) {
    return args -> {
      String locationTable = quoted(appConfig.getLocationTable());
      String customerTable = quoted(appConfig.getDatabaseTable());

      Integer locationRows = jdbcTemplate.queryForObject("select count(*) from " + locationTable, Integer.class);
      Integer customerRows = jdbcTemplate.queryForObject("select count(*) from " + customerTable, Integer.class);

      if (locationRows != null && locationRows == 0) {
        seedLocations(jdbcTemplate, locationTable);
      } else {
        log.info("Skipping location seed. Existing rows={}", locationRows);
      }

      if (customerRows != null && customerRows == 0) {
        seedCustomers(jdbcTemplate, customerTable);
      } else {
        log.info("Skipping customer seed. Existing rows={}", customerRows);
      }
    };
  }

  private void seedLocations(JdbcTemplate jdbcTemplate, String locationTable) {
    String sql = "insert into " + locationTable
        + " (\"hertzLocationCode\", \"displayName\", \"timeZone\") values (?, ?, ?)";

    List<Object[]> batchArgs = new ArrayList<>(LOCATION_COUNT);
    for (int i = 1; i <= LOCATION_COUNT; i++) {
      String code = String.format(Locale.ROOT, "LOC%03d", i);
      String displayName = String.format(Locale.ROOT, "RentalApps Location %03d", i);
      String timeZone = TIME_ZONES[(i - 1) % TIME_ZONES.length];
      batchArgs.add(new Object[] {code, displayName, timeZone});
    }

    jdbcTemplate.batchUpdate(sql, batchArgs);
    log.info("Seeded {} locations into {}", LOCATION_COUNT, locationTable);
  }

  private void seedCustomers(JdbcTemplate jdbcTemplate, String customerTable) {
    String sql = "insert into " + customerTable
        + " (\"id\", \"customerName\", \"locationCode\", \"stall\", \"oneClub\", \"ra\","
        + " \"sourceSystem\", \"arrivalDate\", \"arrivalTime\", \"createdDatetime\", \"updatedDatetime\")"
        + " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    List<Object[]> batchArgs = new ArrayList<>(CUSTOMER_COUNT);
    LocalDateTime now = LocalDateTime.now();

    for (int i = 1; i <= CUSTOMER_COUNT; i++) {
      LocalDateTime arrival = now.minusMinutes(i);
      String locationCode = String.format(Locale.ROOT, "LOC%03d", ((i - 1) % LOCATION_COUNT) + 1);
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
