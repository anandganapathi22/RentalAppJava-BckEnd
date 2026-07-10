package com.rentalapps.vo;

/** Response returned by the AI customer query endpoint. */
public class AiCustomerQueryResponse {
  private String locationId;
  private String question;
  private String modelProvider;
  private String answer;
  private int recordsUsed;
  private int logEventsUsed;

  public String getLocationId() {
    return locationId;
  }

  public void setLocationId(String locationId) {
    this.locationId = locationId;
  }

  public String getQuestion() {
    return question;
  }

  public void setQuestion(String question) {
    this.question = question;
  }

  public String getModelProvider() {
    return modelProvider;
  }

  public void setModelProvider(String modelProvider) {
    this.modelProvider = modelProvider;
  }

  public String getAnswer() {
    return answer;
  }

  public void setAnswer(String answer) {
    this.answer = answer;
  }

  public int getRecordsUsed() {
    return recordsUsed;
  }

  public void setRecordsUsed(int recordsUsed) {
    this.recordsUsed = recordsUsed;
  }

  public int getLogEventsUsed() {
    return logEventsUsed;
  }

  public void setLogEventsUsed(int logEventsUsed) {
    this.logEventsUsed = logEventsUsed;
  }
}
