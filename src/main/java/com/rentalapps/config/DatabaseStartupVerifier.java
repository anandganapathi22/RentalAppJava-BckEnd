package com.rentalapps.config;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.List;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DatabaseStartupVerifier {

  private static final Logger log = LoggerFactory.getLogger(DatabaseStartupVerifier.class);

  @Bean
  public ApplicationRunner verifyConfiguredDatabase(DataSource dataSource, ApplicationConfig appConfig) {
    return args -> {
      List<String> tables = List.of(
          appConfig.getDatabaseTable(),
          appConfig.getShadowTable(),
          appConfig.getLocationTable(),
          appConfig.getShedlockTable(),
          appConfig.getUsersTable()
      );

      try (Connection connection = dataSource.getConnection()) {
        DatabaseMetaData metaData = connection.getMetaData();
        log.info("Datasource initialized: url={}", metaData.getURL());

        for (String tableName : tables) {
          try (ResultSet rs = metaData.getTables(null, null, tableName, null)) {
            log.info("Table check: {} exists={}", tableName, rs.next());
          }
        }
      }
    };
  }
}
