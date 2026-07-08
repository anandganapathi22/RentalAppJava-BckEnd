package com.rentalapps.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.rentalapps.model.CwaMessageBean;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class KafkaRentalEventServiceTest {

  private final CustomerDataService customerDataService = org.mockito.Mockito.mock(CustomerDataService.class);
  private final KafkaRentalEventService service = new KafkaRentalEventService(customerDataService);

  @Test
  void processPersistsDirectJsonRentalEvent() throws Exception {
    String payload = """
        {
          "action": "add",
          "locationCode": "okc11",
          "customerName": "Smith John",
          "oneClub": "OC123",
          "ra": "RA100",
          "stall": "A12",
          "arrivalDate": "07/02/2026",
          "arrivalTime": "10:30"
        }
        """;

    service.process("rental-events-us", null, new RecordHeaders(), payload);

    ArgumentCaptor<CwaMessageBean> captor = ArgumentCaptor.forClass(CwaMessageBean.class);
    verify(customerDataService).persistQueueData(eq("OKC11"), captor.capture());
    assertThat(captor.getValue().rental()).hasSize(1);
    assertThat(captor.getValue().rental().get(0).getCustomerName()).isEqualTo("Smith John");
    assertThat(captor.getValue().rental().get(0).getOneClub()).isEqualTo("OC123");
  }

  @Test
  void processPersistsXmlRentalEventUsingKafkaKeyLocation() throws Exception {
    String payload = """
        <CwaMessageBean>
          <rental>
            <action>add</action>
            <customer>Smith John</customer>
            <oneclub>OC123</oneclub>
            <ra>RA100</ra>
            <stall>A12</stall>
            <arrival-date>07/02/2026</arrival-date>
            <arrival-time>10:30</arrival-time>
          </rental>
        </CwaMessageBean>
        """;

    service.process("rental-events-us", "okc11", new RecordHeaders(), payload);

    ArgumentCaptor<CwaMessageBean> captor = ArgumentCaptor.forClass(CwaMessageBean.class);
    verify(customerDataService).persistQueueData(eq("OKC11"), captor.capture());
    assertThat(captor.getValue().rental()).hasSize(1);
    assertThat(captor.getValue().rental().get(0).getCustomerName()).isEqualTo("Smith John");
  }
}
