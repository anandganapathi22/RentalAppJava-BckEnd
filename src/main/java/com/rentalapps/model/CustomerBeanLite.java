package com.rentalapps.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lightweight customer bean containing only name and stall for the Rental Apps display.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerBeanLite {

  private String customerName;
  private String stall;
}
