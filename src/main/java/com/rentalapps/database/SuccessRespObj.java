package com.rentalapps.database;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.List;

/**
 * Success response wrapper containing a message and a list of location data.
 */
public class SuccessRespObj {

  String message;
  List<GbLocationRespObj> data;

  public String toString() {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    return gson.toJson(this);
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public List<GbLocationRespObj> getData() {
    return data;
  }

  public void setData(List<GbLocationRespObj> data) {
    this.data = data;
  }
}