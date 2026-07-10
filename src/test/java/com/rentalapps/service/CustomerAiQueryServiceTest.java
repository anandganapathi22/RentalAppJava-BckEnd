package com.rentalapps.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
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
}
