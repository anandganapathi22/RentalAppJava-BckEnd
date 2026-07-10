package com.rentalapps.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentalapps.vo.AiCustomerQueryResponse;
import com.rentalapps.vo.CustomerBean;
import com.rentalapps.vo.GbLocationRespObj;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Read-only AI query service for answering questions over customer stall data. */
@Service
public class CustomerAiQueryService {
  private static final String SYSTEM_PROMPT = """
      You are the Rental Applications analytics assistant.
      Answer questions about rental customer data, stall assignments, duplicates, errors, and exceptions.
      Use only the database records and log excerpts supplied in the user message.
      Do not invent customers, locations, stalls, rental agreement numbers, errors, exceptions, or timestamps.
      If the supplied records or log excerpts do not contain the answer, say that the provided data does not show it.
      Keep answers concise. Include customer names, stalls, rental agreements, and exception summaries when relevant.
      """;
  private static final int MAX_CUSTOMERS_IN_PROMPT = 120;
  private static final int MAX_LOG_LINES = 80;
  private static final int MAX_LOG_LINES_IN_DIRECT_ANSWER = 12;

  private final CustomerDataService customerDataService;
  private final ObjectMapper objectMapper;
  private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;
  private final List<String> logPaths;

  public CustomerAiQueryService(CustomerDataService customerDataService,
                                ObjectMapper objectMapper,
                                ObjectProvider<ChatClient.Builder> chatClientBuilderProvider,
                                @Value("${rental.analytics.log-paths:backend.err.log,backend.out.log}")
                                String logPaths) {
    this.customerDataService = customerDataService;
    this.objectMapper = objectMapper;
    this.chatClientBuilderProvider = chatClientBuilderProvider;
    this.logPaths = Stream.of(StringUtils.defaultString(logPaths).split(","))
        .map(StringUtils::trimToNull)
        .filter(Objects::nonNull)
        .toList();
  }

  public AiCustomerQueryResponse query(String locationId, String question) throws IOException {
    if (StringUtils.isBlank(locationId)) {
      throw new IllegalArgumentException("locationId is required");
    }
    if (StringUtils.isBlank(question)) {
      throw new IllegalArgumentException("question is required");
    }

    String requestedLocationId = locationId.trim();
    String trimmedQuestion = question.trim();

    if (isLogQuestion(trimmedQuestion)) {
      List<String> logLines = readRelevantLogLines();
      return createResponse(
          requestedLocationId,
          trimmedQuestion,
          createLogAnswer(logLines),
          0,
          logLines.size());
    }

    List<GbLocationRespObj> locations = getLocationsSafely();
    String effectiveLocationId = findMentionedLocationId(trimmedQuestion, locations).orElse(requestedLocationId);
    List<CustomerBean> customers = customerDataService.getRentalAppsData2(effectiveLocationId);
    List<GbLocationRespObj> matchingLocations = locations.stream()
        .filter(location -> StringUtils.equalsIgnoreCase(location.getHertzLocationCode(), effectiveLocationId))
        .toList();

    if (isCustomerCountQuestion(trimmedQuestion)) {
      return createResponse(
          effectiveLocationId,
          trimmedQuestion,
          "%s has %d customer records.".formatted(effectiveLocationId, customers.size()),
          customers.size(),
          0);
    }

    List<String> logLines = readRelevantLogLines();
    String analyticsContext = buildAnalyticsContext(effectiveLocationId, customers, matchingLocations, logLines);
    String customerJson = toJson(limitCustomers(customers));
    ChatClient.Builder chatClientBuilder = getChatClientBuilder();
    if (chatClientBuilder == null) {
      throw new IllegalStateException("AI chat model is not enabled");
    }

    String answer = chatClientBuilder.build()
        .prompt()
        .system(SYSTEM_PROMPT)
        .user("""
            Location ID: %s
            Analytics context:
            %s

            Customer records JSON:
            %s

            Relevant recent log lines:
            %s

            Question: %s
            """.formatted(effectiveLocationId, analyticsContext, customerJson, String.join("\n", logLines), trimmedQuestion))
        .call()
        .content();

    return createResponse(
        effectiveLocationId,
        trimmedQuestion,
        StringUtils.trimToEmpty(answer),
        customers.size(),
        logLines.size());
  }

  private AiCustomerQueryResponse createResponse(String locationId,
                                                 String question,
                                                 String answer,
                                                 int recordsUsed,
                                                 int logEventsUsed) {
    AiCustomerQueryResponse response = new AiCustomerQueryResponse();
    response.setLocationId(locationId);
    response.setQuestion(question);
    response.setAnswer(answer);
    response.setRecordsUsed(recordsUsed);
    response.setLogEventsUsed(logEventsUsed);
    return response;
  }

  private ChatClient.Builder getChatClientBuilder() {
    try {
      return chatClientBuilderProvider.getIfAvailable();
    } catch (BeansException e) {
      return null;
    }
  }

  private List<GbLocationRespObj> getLocationsSafely() {
    try {
      return customerDataService.getLocations();
    } catch (Exception e) {
      return List.of();
    }
  }

  private Optional<String> findMentionedLocationId(String question, List<GbLocationRespObj> locations) {
    String normalizedQuestion = question.toUpperCase(Locale.ROOT);
    return locations.stream()
        .map(GbLocationRespObj::getHertzLocationCode)
        .map(StringUtils::trimToNull)
        .filter(Objects::nonNull)
        .filter(locationCode -> normalizedQuestion.contains(locationCode.toUpperCase(Locale.ROOT)))
        .findFirst();
  }

  private boolean isCustomerCountQuestion(String question) {
    String normalizedQuestion = question.toLowerCase(Locale.ROOT);
    boolean asksForCount = normalizedQuestion.contains("how many")
        || normalizedQuestion.contains("count")
        || normalizedQuestion.contains("number of")
        || normalizedQuestion.contains("total");
    boolean targetsCustomers = normalizedQuestion.contains("customer")
        || normalizedQuestion.contains("customers")
        || normalizedQuestion.contains("records");

    return asksForCount && targetsCustomers;
  }

  private boolean isLogQuestion(String question) {
    String normalizedQuestion = question.toLowerCase(Locale.ROOT);
    boolean asksAboutLogs = normalizedQuestion.contains("log")
        || normalizedQuestion.contains("logs")
        || normalizedQuestion.contains("backend")
        || normalizedQuestion.contains("docker");
    boolean asksAboutProblems = normalizedQuestion.contains("error")
        || normalizedQuestion.contains("errors")
        || normalizedQuestion.contains("exception")
        || normalizedQuestion.contains("exceptions")
        || normalizedQuestion.contains("warn")
        || normalizedQuestion.contains("warning")
        || normalizedQuestion.contains("failed")
        || normalizedQuestion.contains("failure");

    return asksAboutLogs && asksAboutProblems;
  }

  private String createLogAnswer(List<String> logLines) {
    if (logLines.isEmpty()) {
      return "No readable backend error, warning, exception, or failure log lines were found in the configured analytics log paths.";
    }

    List<String> recentLines = logLines.stream()
        .skip(Math.max(0, logLines.size() - MAX_LOG_LINES_IN_DIRECT_ANSWER))
        .toList();

    return "Yes. I found %d relevant backend log lines. Recent entries:%n%s".formatted(
        logLines.size(),
        recentLines.stream()
            .map(line -> "- " + line)
            .collect(Collectors.joining(System.lineSeparator())));
  }

  private String buildAnalyticsContext(String locationId,
                                       List<CustomerBean> customers,
                                       List<GbLocationRespObj> locations,
                                       List<String> logLines) throws JsonProcessingException {
    long assignedStalls = customers.stream().filter(customer -> StringUtils.isNotBlank(customer.getStall())).count();
    long missingStalls = customers.size() - assignedStalls;
    long missingNames = customers.stream().filter(customer -> StringUtils.isBlank(customer.getCustomerName())).count();
    List<Map.Entry<String, Long>> duplicateRentalAgreements = customers.stream()
        .map(CustomerBean::getRa)
        .map(StringUtils::trimToNull)
        .filter(Objects::nonNull)
        .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
        .entrySet()
        .stream()
        .filter(entry -> entry.getValue() > 1)
        .sorted(Map.Entry.comparingByKey())
        .toList();

    Map<String, Object> context = Map.of(
        "locationId", locationId,
        "location", locations.isEmpty() ? Map.of() : locations.get(0),
        "totalCustomers", customers.size(),
        "assignedStalls", assignedStalls,
        "missingStalls", missingStalls,
        "missingCustomerNames", missingNames,
        "duplicateRentalAgreements", duplicateRentalAgreements,
        "logEventsIncluded", logLines.size()
    );

    return objectMapper.writeValueAsString(context);
  }

  private List<CustomerBean> limitCustomers(List<CustomerBean> customers) {
    if (customers.size() <= MAX_CUSTOMERS_IN_PROMPT) {
      return customers;
    }

    return customers.subList(0, MAX_CUSTOMERS_IN_PROMPT);
  }

  private List<String> readRelevantLogLines() {
    return logPaths.stream()
        .map(Path::of)
        .filter(Files::isRegularFile)
        .flatMap(this::safeReadLines)
        .filter(this::isRelevantLogLine)
        .sorted(Comparator.reverseOrder())
        .limit(MAX_LOG_LINES)
        .sorted()
        .toList();
  }

  private Stream<String> safeReadLines(Path path) {
    try {
      return Files.readAllLines(path).stream()
          .map(line -> "%s: %s".formatted(path.getFileName(), line));
    } catch (IOException e) {
      return Stream.empty();
    }
  }

  private boolean isRelevantLogLine(String line) {
    String normalizedLine = line.toLowerCase();
    return normalizedLine.contains("error")
        || normalizedLine.contains("exception")
        || normalizedLine.contains("warn")
        || normalizedLine.contains("failed");
  }

  private String toJson(List<CustomerBean> customers) throws JsonProcessingException {
    return objectMapper.writeValueAsString(customers);
  }
}
