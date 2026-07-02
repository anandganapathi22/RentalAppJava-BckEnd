package com.rentalapps.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlCData;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import java.util.ArrayList;
import java.util.List;

/**
 * Bean representing a CWA XML message containing a list of rental operations.
 */
public class CwaMessageBean {
  @JacksonXmlElementWrapper(localName = "rental")
  @JacksonXmlCData
  private final List<Rental> rental = new ArrayList<>();


  public List<Rental> rental() {
    return rental;
  }


  //@JsonSetter
  public void setRental(Rental rental) {
    this.rental.add(rental);
  }

  @Override
  public String toString() {
    return "CWAMessageBean [rental=" + rental + "]";
  }

}
