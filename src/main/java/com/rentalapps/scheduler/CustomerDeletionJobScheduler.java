package com.rentalapps.scheduler;

import com.rentalapps.config.ApplicationConfig;
import com.rentalapps.service.DbService;
import com.rentalapps.util.RentalDateTimeUtils;
import java.time.Duration;
import java.time.Instant;
import net.javacrumbs.shedlock.core.DefaultLockingTaskExecutor;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job that periodically removes old customer data from the database.
 * Runs every 30 minutes and deletes customer records based on region-specific logic.
 * Uses programmatic ShedLock with a region-aware lock name to prevent conflicts
 * when US and EU deployments share the same shedlock table.
 */
@Component
@ConditionalOnProperty(name = "gb.config.enableDeletionScheduler", havingValue = "true")
public class CustomerDeletionJobScheduler {

  private static final Logger logger = LoggerFactory.getLogger(CustomerDeletionJobScheduler.class);

  @Autowired
  private DbService dbService;

  @Autowired
  private ApplicationConfig appConfig;

  private final LockingTaskExecutor lockingTaskExecutor;

  public CustomerDeletionJobScheduler(LockProvider lockProvider) {
    this.lockingTaskExecutor = new DefaultLockingTaskExecutor(lockProvider);
  }

  @Scheduled(fixedDelay = 30 * 60 * 1000, initialDelay = 1000)
  public void scheduleTask() {

    if (!appConfig.isEnableDeletionScheduler()) {
      logger.info("CustomerDeletionJobScheduler DISABLED | time={}", RentalDateTimeUtils.getCurrentUtcTime());
      return;
    }

    String region = appConfig.getDeletionRegion();
    String lockName = "customerDeletionJob_" + region;

    LockConfiguration lockConfig = new LockConfiguration(
        Instant.now(), lockName, Duration.ofMinutes(25), Duration.ofMinutes(5));

    lockingTaskExecutor.executeWithLock((Runnable) () -> {
      logger.info("CustomerDeletionJobScheduler START | region={} | time={}",
          region, RentalDateTimeUtils.getCurrentUtcTime());
      try {
        dbService.removeOldCustomerData();
      } catch (Exception ex) {
        logger.error("CustomerDeletionJobScheduler ERROR | region={} | message={}",
            region, ex.getMessage(), ex);
      }
      logger.info("CustomerDeletionJobScheduler END | region={} | time={}",
          region, RentalDateTimeUtils.getCurrentUtcTime());
    }, lockConfig);
  }
}
