package com.rentalapps.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentalapps.vo.AiCustomerQueryResponse;
import com.rentalapps.vo.CustomerBean;
import java.io.IOException;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/** Read-only AI query service for answering questions over customer stall data. */
@Service
public class CustomerAiQueryService {
  private static final String SYSTEM_PROMPT = """
      You answer questions about rental customer stall assignments.
      Use only the customer records supplied in the user message.
      Do not invent customers, locations, stalls, rental agreement numbers, or arrival details.
      If the supplied records do not contain the answer, say that the data provided does not show it.
      Keep answers concise and include customer names and stalls when relevant.
      """;

  private final CustomerDataService customerDataService;
  private final ObjectMapper objectMapper;
  private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;

  public CustomerAiQueryService(CustomerDataService customerDataService,
                                ObjectMapper objectMapper,
                                ObjectProvider<ChatClient.Builder> chatClientBuilderProvider) {
    this.customerDataService = customerDataService;
    this.objectMapper = objectMapper;
    this.chatClientBuilderProvider = chatClientBuilderProvider;
  }

  public AiCustomerQueryResponse query(String locationId, String question) throws IOException {
    if (StringUtils.isBlank(locationId)) {
      throw new IllegalArgumentException("locationId is required");
    }
    if (StringUtils.isBlank(question)) {
      throw new IllegalArgumentException("question is required");
    }

    ChatClient.Builder chatClientBuilder = chatClientBuilderProvider.getIfAvailable();
    if (chatClientBuilder == null) {
      throw new IllegalStateException("Spring AI chat model is not enabled");
    }

    List<CustomerBean> customers = customerDataService.getRentalAppsData2(locationId.trim());
    String customerJson = toJson(customers);
    String answer = chatClientBuilder.build()
        .prompt()
        .system(SYSTEM_PROMPT)
        .user("""
            Location ID: %s
            Customer records JSON:
            %s

            Question: %s
            """.formatted(locationId.trim(), customerJson, question.trim()))
        .call()
        .content();

    AiCustomerQueryResponse response = new AiCustomerQueryResponse();
    response.setLocationId(locationId.trim());
    response.setQuestion(question.trim());
    response.setAnswer(StringUtils.trimToEmpty(answer));
    response.setRecordsUsed(customers.size());
    return response;
  }

  private String toJson(List<CustomerBean> customers) throws JsonProcessingException {
    return objectMapper.writeValueAsString(customers);
  }
}
