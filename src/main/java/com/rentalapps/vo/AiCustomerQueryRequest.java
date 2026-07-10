package com.rentalapps.vo;

/** Request body for natural-language customer data queries. */
public class AiCustomerQueryRequest {
  private String locationId;
  private String question;
  private String modelProvider;

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
}
