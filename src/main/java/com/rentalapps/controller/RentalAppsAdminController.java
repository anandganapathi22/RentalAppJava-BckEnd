package com.rentalapps.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * MVC controller that serves the React UI index page for admin routes.
 */
@Controller
public class RentalAppsAdminController {

  /** Forwards root, /admin, and /admin/{path} requests to the React SPA index page. */
  @RequestMapping(value = {"/", "/admin", "/admin/{x:[\\w\\-]+}"})
  public String getIndex(HttpServletRequest request) {
    return "/index.html";
  }

}
