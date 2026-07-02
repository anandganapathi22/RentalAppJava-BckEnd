package com.rentalapps.vo;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Bean representing a mapping between a location code and its hex-encoded correlation ID.
 */
@Data
@NoArgsConstructor
public class CorrelationBean {

  private String hertzLocationCode = "";
  private String correlationId = "";
}
