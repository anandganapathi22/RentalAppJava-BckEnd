package com.rentalapps.database;

import com.google.gson.Gson;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Business object representing local time information for a location,
 * including continent, city, and timezone details.
 */
@Data
@NoArgsConstructor
public class GbLocaltimeBo {

  private String continent = "";
  private String city = "";
  private String localDateTime = "";
  private String timeZone = "";
  private String reference = "";
  
  public GbLocaltimeBo(String json) {
    Gson gson = new Gson();
    GbLocaltimeBo tempLocation = gson.fromJson(json, GbLocaltimeBo.class);
    this.continent = tempLocation.getContinent();
    this.city = tempLocation.getCity();
  }

}
