package com.rentalapps.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request object for location CRUD operations (add, update, delete).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GbLocationReqObj {

  private String hertzLocationCode = "";
  private String displayName = "";
  private String timeZone = "";
}
