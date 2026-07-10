package com.rentalapps.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentalapps.vo.AiCustomerQueryResponse;
import com.rentalapps.vo.CustomerBean;
import com.rentalapps.vo.GbLocationRespObj;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;

class CustomerAiQueryServiceTest {

  @Test
  void queryReturnsClearErrorWhenChatModelIsDisabled() {
    CustomerDataService customerDataService = mock(CustomerDataService.class);
    ObjectProvider<ChatClient.Builder> chatClientBuilderProvider = mock(ObjectProvider.class);
    when(chatClientBuilderProvider.getIfAvailable()).thenReturn(null);

    CustomerAiQueryService service = new CustomerAiQueryService(
        customerDataService,
        new ObjectMapper(),
        chatClientBuilderProvider,
        "");

    IllegalStateException exception = assertThrows(
        IllegalStateException.class,
        () -> service.query("MNMIN10", "Who is assigned to stall 42?"));

    assertEquals("AI chat model is not enabled", exception.getMessage());
  }

  @Test
  void queryReturnsDatabaseCountForMentionedLocationWithoutCallingAiModel() throws Exception {
    CustomerDataService customerDataService = mock(CustomerDataService.class);
    ObjectProvider<ChatClient.Builder> chatClientBuilderProvider = mock(ObjectProvider.class);
    when(chatClientBuilderProvider.getIfAvailable()).thenReturn(null);
    when(customerDataService.getLocations()).thenReturn(List.of(
        new GbLocationRespObj("AZPHO11", "Phoenix", "America/Phoenix"),
        new GbLocationRespObj("CASFO15", "San Francisco", "America/Los_Angeles")));
    when(customerDataService.getRentalAppsData2("CASFO15")).thenReturn(List.of(
        new CustomerBean("Customer One", "S-001"),
        new CustomerBean("Customer Two", "S-002")));

    CustomerAiQueryService service = new CustomerAiQueryService(
        customerDataService,
        new ObjectMapper(),
        chatClientBuilderProvider,
        "");

    AiCustomerQueryResponse response = service.query(
        "AZPHO11",
        "how many customers are there for CASFO15?");

    assertEquals("CASFO15", response.getLocationId());
    assertEquals("CASFO15 has 2 customer records.", response.getAnswer());
    assertEquals(2, response.getRecordsUsed());
  }
}
