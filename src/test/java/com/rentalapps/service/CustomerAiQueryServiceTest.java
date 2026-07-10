package com.rentalapps.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentalapps.vo.AiCustomerQueryResponse;
import com.rentalapps.vo.CustomerBean;
import com.rentalapps.vo.GbLocationRespObj;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CustomerAiQueryServiceTest {

  @TempDir
  Path tempDir;

  @Test
  void queryReturnsClearErrorWhenChatGptIsSelectedWithoutApiKey() throws Exception {
    CustomerDataService customerDataService = mock(CustomerDataService.class);
    when(customerDataService.getLocations()).thenReturn(List.of(
        new GbLocationRespObj("MNMIN10", "Minneapolis", "America/Chicago")));
    when(customerDataService.getRentalAppsData2("MNMIN10")).thenReturn(List.of());

    CustomerAiQueryService service = createService(customerDataService, "");

    IllegalStateException exception = assertThrows(
        IllegalStateException.class,
        () -> service.query("MNMIN10", "Who is assigned to stall 42?", "openai"));

    assertEquals("ChatGPT is not configured. Set OPENAI_API_KEY before selecting ChatGPT.", exception.getMessage());
  }

  @Test
  void queryReturnsDatabaseCountForMentionedLocationWithoutCallingAiModel() throws Exception {
    CustomerDataService customerDataService = mock(CustomerDataService.class);
    when(customerDataService.getLocations()).thenReturn(List.of(
        new GbLocationRespObj("AZPHO11", "Phoenix", "America/Phoenix"),
        new GbLocationRespObj("CASFO15", "San Francisco", "America/Los_Angeles")));
    when(customerDataService.getRentalAppsData2("CASFO15")).thenReturn(List.of(
        new CustomerBean("Customer One", "S-001"),
        new CustomerBean("Customer Two", "S-002")));

    CustomerAiQueryService service = createService(customerDataService, "");

    AiCustomerQueryResponse response = service.query(
        "AZPHO11",
        "how many customers are there for CASFO15?",
        "chatgpt");

    assertEquals("CASFO15", response.getLocationId());
    assertEquals("openai", response.getModelProvider());
    assertEquals("CASFO15 has 2 customer records.", response.getAnswer());
    assertEquals(2, response.getRecordsUsed());
  }

  @Test
  void queryAnswersBackendLogQuestionWithoutCallingAiModel() throws Exception {
    Path logFile = tempDir.resolve("backend.log");
    Files.write(logFile, List.of(
        "2026-07-10 INFO Started RentalAppsListenerApplication",
        "2026-07-10 ERROR Application run failed",
        "Caused by: org.h2.mvstore.MVStoreException: The file is locked"));

    CustomerDataService customerDataService = mock(CustomerDataService.class);
    CustomerAiQueryService service = createService(customerDataService, logFile.toString());

    AiCustomerQueryResponse response = service.query(
        "AZPHO11",
        "are there any errors in the docker back end logs?",
        "ollama");

    assertEquals("AZPHO11", response.getLocationId());
    assertEquals("ollama", response.getModelProvider());
    assertEquals(0, response.getRecordsUsed());
    assertEquals(2, response.getLogEventsUsed());
    assertTrue(response.getAnswer().contains("Application run failed"));
    assertTrue(response.getAnswer().contains("The file is locked"));
  }

  private CustomerAiQueryService createService(CustomerDataService customerDataService, String logPaths) {
    return new CustomerAiQueryService(
        customerDataService,
        new ObjectMapper(),
        logPaths,
        "http://localhost:11434",
        "llama3.1",
        "",
        "gpt-4o-mini");
  }
}
