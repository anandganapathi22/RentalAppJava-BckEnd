package com.rentalapps.database;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Business object representing a Rental Apps location record from the database.
 * Supports construction from JSON via Gson deserialization.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GbLocationBo {
 
  private String hertzLocationCode = "";
  private String displayName = "";
  private String timeZone = "";

  public GbLocationBo(String json) {
    Gson gson = new Gson();
    GbLocationBo tempLocation = gson.fromJson(json, GbLocationBo.class);
    this.hertzLocationCode = tempLocation.hertzLocationCode;
    this.displayName = tempLocation.displayName;
    this.timeZone = tempLocation.timeZone;
  }

  @Override
  public String toString() {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    return gson.toJson(this);
  }
}
