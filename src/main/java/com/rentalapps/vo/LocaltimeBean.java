package com.rentalapps.vo;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Bean representing local time information for a geographic location.
 */
@Data
@NoArgsConstructor
public class LocaltimeBean {

  private String continent = "";
  private String city = "";
  private String localDateTime = "";
  private String timeZone = "";
  private String reference = "";
}
