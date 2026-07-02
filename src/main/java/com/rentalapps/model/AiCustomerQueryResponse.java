package com.rentalapps.model;

/** Response returned by the AI customer query endpoint. */
public class AiCustomerQueryResponse {
  private String locationId;
  private String question;
  private String answer;
  private int recordsUsed;

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
}
