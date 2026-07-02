package com.rentalapps.model;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Bean representing a location with its code and display name.
 */
@Data
@NoArgsConstructor
public class LocationBean {

  private String hertzLocationCode = "";
  private String displayName = "";
}
