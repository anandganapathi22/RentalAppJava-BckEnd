package com.rentalapps.database;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response object representing a location record returned to API callers.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GbLocationRespObj {

  private String hertzLocationCode = "";
  private String displayName = "";
  private String timeZone = "";
}
