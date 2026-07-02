package com.rentalapps.service;

import com.rentalapps.exception.DatabaseException;
import com.rentalapps.vo.GbCustomerReqObj;
import com.rentalapps.vo.GbCustomerRespObj;
import java.util.List;
import org.junit.Assert;
import org.junit.Ignore;

public class DbServiceTest {

  @Ignore
  public void testGetCustomerListTotalRecordCount() {

    DbService dbService = new DbService();

    try {

      List<GbCustomerRespObj> output = dbService.getCustomerList();
      Assert.assertEquals(3, output.size());

    } catch (DatabaseException ex) {

      ex.printStackTrace();
    }
  }

  @Ignore
  public void testGetCustomerListAllFirstRecord() {

    DbService dbservice = new DbService();

    try {

      List<GbCustomerRespObj> output = dbservice.getCustomerList();
      Assert.assertEquals("CALAX15#Saparshi Chak#333333OC", output.get(0).getId());
      Assert.assertEquals("23/08/2023", output.get(0).getArrivalDate());
      Assert.assertEquals("STALL", output.get(0).getStall());
      Assert.assertEquals("CALAX15", output.get(0).getLocationCode());
      Assert.assertEquals("Saptarshi Chak", output.get(0).getCustomerName());
      Assert.assertEquals("333333OC", output.get(0).getOneClub());
      Assert.assertEquals("333333RA", output.get(0).getRa());
      Assert.assertEquals("22:33", output.get(0).getArrivalTime());

    } catch (DatabaseException ex) {

      ex.printStackTrace();
    }
  }

  @Ignore
  public void testGetCustomerListByLocationCode() {

    DbService dbservice = new DbService();

    try {

      List<GbCustomerRespObj> output = dbservice.getCustomerList("CALAX15");
      Assert.assertEquals("CALAX15#Saparshi Chak#333333OC", output.get(0).getId());
      Assert.assertEquals("23/08/2023", output.get(0).getArrivalDate());
      Assert.assertEquals("STALL", output.get(0).getStall());
      Assert.assertEquals("CALAX15", output.get(0).getLocationCode());
      Assert.assertEquals("Saptarshi Chak", output.get(0).getCustomerName());
      Assert.assertEquals("333333OC", output.get(0).getOneClub());
      Assert.assertEquals("333333RA", output.get(0).getRa());
      Assert.assertEquals("22:33", output.get(0).getArrivalTime());

    } catch (DatabaseException ex) {

      ex.printStackTrace();
    }
  }

  @Ignore
  public void testGetCustomerById() {

    DbService dbservice = new DbService();

    try {

      GbCustomerRespObj output = dbservice.getCustomer("CALAX15#Saparshi Chak#333333OC");
      Assert.assertEquals("CALAX15#Saparshi Chak#333333OC", output.getId());
      Assert.assertEquals("23/08/2023", output.getArrivalDate());
      Assert.assertEquals("STALL", output.getStall());
      Assert.assertEquals("CALAX15", output.getLocationCode());
      Assert.assertEquals("Saptarshi Chak", output.getCustomerName());
      Assert.assertEquals("333333OC", output.getOneClub());
      Assert.assertEquals("333333RA", output.getRa());
      Assert.assertEquals("22:33", output.getArrivalTime());

    } catch (DatabaseException ex) {

      ex.printStackTrace();
    }
  }

  @Ignore
  public void testupdateCustomer() {

    GbCustomerReqObj input = new GbCustomerReqObj();
    String id = "OKOKC11#Partha Banerjee#222222OC";
    input.setId(id);
    input.setCustomerName("Partha Sarathi Banerjee");
    DbService dbservice = new DbService();

    try {

      GbCustomerRespObj output = dbservice.updateCustomer(input);
      Assert.assertEquals("Partha Banerjee", output.getCustomerName());
      GbCustomerRespObj output2 = dbservice.getCustomer(id);
      Assert.assertEquals("Partha Sarathi Banerjee", output2.getCustomerName());

    } catch (DatabaseException ex) {

      ex.printStackTrace();
    }
  }

  @Ignore
  public void testaddCustomer() {

    GbCustomerReqObj input = new GbCustomerReqObj();
    String id = "OKOKC11#IBM X#444444OC";
    input.setId(id);
    input.setCustomerName("IBM X");
    input.setLocationCode("OKOKC11");
    input.setStall("GOLD");
    input.setOneClub("444444OC");
    input.setRa("444444RA");
    input.setArrivalDate("03/08/2023");
    input.setArrivalTime("44:44");
    DbService dbservice = new DbService();

    try {

      GbCustomerRespObj output = dbservice.addCustomer(input);
      Assert.assertEquals("OKOKC11#IBM X#444444OC", output.getId());
      Assert.assertEquals("03/08/2023", output.getArrivalDate());
      Assert.assertEquals("GOLD", output.getStall());
      Assert.assertEquals("OKOKC11", output.getLocationCode());
      Assert.assertEquals("IBM X", output.getCustomerName());
      Assert.assertEquals("444444OC", output.getOneClub());
      Assert.assertEquals("444444RA", output.getRa());
      Assert.assertEquals("44:44", output.getArrivalTime());

    } catch (DatabaseException ex) {

      ex.printStackTrace();
    }
  }

  @Ignore
  public void testremoveCustomer() {

    GbCustomerReqObj input = new GbCustomerReqObj();
    String id = "OKOKC11#IBM X#444444OC";
    input.setId(id);
    DbService dbservice = new DbService();

    try {

      GbCustomerRespObj output = dbservice.removeCustomer(input);
      Assert.assertEquals("OKOKC11#IBM X#444444OC", output.getId());
      Assert.assertEquals("03/08/2023", output.getArrivalDate());
      Assert.assertEquals("GOLD", output.getStall());
      Assert.assertEquals("OKOKC11", output.getLocationCode());
      Assert.assertEquals("IBM X", output.getCustomerName());
      Assert.assertEquals("444444OC", output.getOneClub());
      Assert.assertEquals("444444RA", output.getRa());
      Assert.assertEquals("44:44", output.getArrivalTime());

    } catch (DatabaseException ex) {

      ex.printStackTrace();
    }
  }

}
