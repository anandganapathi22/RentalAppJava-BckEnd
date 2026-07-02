package com.rentalapps.config;

import com.rentalapps.util.DatabaseConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Centralized application configuration that reads properties from the Spring environment.
 * Provides access to database table names, feature flags, and business rule settings.
 */
@Configuration
public class ApplicationConfig {
  @Autowired
  private Environment env;

  /** Returns the customer table name. */
  public String getDatabaseTable() {
    return env.getProperty("db.tables.customer");
  }

  /** Returns the audit table name. */
  public String getShadowTable() {
    return env.getProperty("db.tables.audit");
  }

  /** Returns the location table name. */
  public String getLocationTable() {
    return env.getProperty("db.tables.location");
  }

  /** Returns the ShedLock table name. */
  public String getShedlockTable() {
    return env.getProperty("db.tables.shedlock");
  }

  /** Returns the max character length used when truncating first names for display. */
  public String getFirstNameLengthCut() {
    return env.getProperty("gb.config.firstNameLengthCut");
  }

  /** Returns the age threshold (in minutes) after which customer records are eligible for deletion. */
  public String getTimeWindow() {
    return env.getProperty("gb.config.oldnessOfCustomerDataInMinutes");
  }

  /** Returns whether the scheduled customer deletion job is enabled. */
  public boolean isEnableDeletionScheduler() {
    return Boolean.parseBoolean(env.getProperty("gb.config.enableDeletionScheduler", "false"));
  }

  /** Returns the region (US or EU) for which the deletion scheduler operates. */
  public String getDeletionRegion() {
    String region = env.getProperty("gb.config.deletionRegion");

    if (region == null || region.trim().isEmpty()) {
      throw new IllegalStateException("Missing required config: gb.config.deletionRegion");
    }

    region = region.trim().toUpperCase();

    if (!DatabaseConstants.RENTALAPPS_REGION_US.equals(region) && !DatabaseConstants.RENTALAPPS_REGION_EU.equals(region)) {
      throw new IllegalStateException(
              "Invalid value for gb.config.deletionRegion: " + region
                      + ". Expected " + DatabaseConstants.RENTALAPPS_REGION_US + " or " + DatabaseConstants.RENTALAPPS_REGION_EU);
    }

    return region;
  }

  /** Returns whether DASH Qual/Staging testing mode is enabled (RA values starting with 'T'). */
  public boolean isEnableQualTesting() {
    return Boolean.parseBoolean(env.getProperty("gb.config.enableQualTesting", "false"));
  }

  /** Returns the TTL expiry period in months for shadow table records. */
  public int getShadowTableExpiryMonths() {
    return Integer.parseInt(env.getProperty("gb.config.shadowTableExpiryMonths", "3"));
  }

  /** Returns whether shadow table audit logging is enabled. */
  public boolean isShadowTableEnable() {
    return Boolean.parseBoolean(env.getProperty("gb.config.shadowTableEnable", "true"));
  }
}
