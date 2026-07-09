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
  private static final String[] LOCATION_CODES = {
      "MNMIN10", "OKOKC11", "FLFMY11", "CAOAK12", "CODEN11", "NYLGA10", "FLTAM11", "AZPHO11",
      "CASJO11", "FLWES11", "KYLOU11", "OHCLE12", "TXSAT11", "NMABQ11", "CASDI11", "RIPRO11",
      "TXAUS15", "NCRAL11", "TXELP11", "MOSTL11", "NCCHA12", "DCIAD26", "MALOG11", "FLMIA15",
      "NVLAS11", "MDBAL11", "FLFLA11", "TXLOV11", "CABUR11", "DCDCA11", "TXIAH12", "MOKAN11",
      "TNNAS11", "TXDFW20", "ILMDW11", "NYJFK10", "WIMIL12", "CASAC11", "HIHON11", "CTHAR11",
      "VARIC11", "GAATL11", "CAONT10", "TXHOB24", "PAPIT11", "OHCIN11", "ILORD10", "UTSAL11",
      "CASFO15", "MIDAP13", "HIKAH10", "LANEW13", "HIKAU10", "MNMIN11", "WASEA11", "ORPDX11",
      "TNMEM11", "HIKON11", "ININD11", "PAPHI11", "FLORL16", "CALAX15", "NJNEW11", "CNTOR11",
      "CNTOR18", "CNMON11", "IADES11"
  };
  private static final int LOCATION_COUNT = LOCATION_CODES.length;
  private static final int CUSTOMER_COUNT = 1000;
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
    for (String code : LOCATION_CODES) {
      String displayName = code;
      String timeZone = timeZoneFor(code);
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
      String locationCode = LOCATION_CODES[(i - 1) % LOCATION_CODES.length];
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

  private String timeZoneFor(String locationCode) {
    String region = locationCode.substring(0, 2);
    return switch (region) {
      case "AZ", "CA", "NV", "OR", "WA" -> "America/Los_Angeles";
      case "CO", "NM", "UT" -> "America/Denver";
      case "AL", "AR", "IA", "IL", "IN", "KY", "LA", "MI", "MN", "MO", "TN", "TX", "WI" -> "America/Chicago";
      case "CN", "CT", "DC", "FL", "GA", "MA", "MD", "NC", "NJ", "NY", "OH", "PA", "RI", "VA" -> "America/New_York";
      case "HI" -> "Pacific/Honolulu";
      default -> "America/Chicago";
    };
  }
}
