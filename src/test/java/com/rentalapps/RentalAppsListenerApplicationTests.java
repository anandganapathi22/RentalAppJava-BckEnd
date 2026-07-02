package com.rentalapps;

import net.javacrumbs.shedlock.core.LockProvider;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(properties = {
    "goldSign.MQ.host.primary=localhost",
    "goldSign.MQ.port.primary=1414",
    "goldSign.MQ.queue.manager.primary=QM1",
    "goldSign.MQ.channel.primary=DEV.APP.SVRCONN",
    "goldSign.MQ.request.queue.primary=DEV.QUEUE.1",
    "goldSign.MQ.host.secondary=localhost",
    "goldSign.MQ.port.secondary=1415",
    "goldSign.MQ.queue.manager.secondary=QM2",
    "goldSign.MQ.channel.secondary=DEV.APP.SVRCONN",
    "goldSign.MQ.request.queue.secondary=DEV.QUEUE.2",
    "goldSign.MQ.consumer.locationsFilter=JMSCorrelationID='ID:TEST'",
    "spring.datasource.url=jdbc:h2:mem:rentalapps-test;MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "db.tables.customer=rentalapps-customer-data",
    "db.tables.audit=rentalapps-audit-data",
    "db.tables.location=rentalapps-locations-data",
    "db.tables.shedlock=rentalapps-shedlock-data",
    "gb.config.firstNameLengthCut=2",
    "gb.config.oldnessOfCustomerDataInMinutes=60",
    "gb.config.enableDeletionScheduler=false",
    "gb.config.deletionRegion=US",
    "gb.config.enableQualTesting=false",
    "gb.config.shadowTableEnable=false",
    "gb.config.shadowTableExpiryMonths=3"
})
class RentalAppsListenerApplicationTests {
  @MockBean
  private LockProvider lockProvider;

  @Test
  void contextLoads() {
  }
}
