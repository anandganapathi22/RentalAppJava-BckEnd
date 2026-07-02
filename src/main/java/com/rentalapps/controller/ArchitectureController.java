package com.rentalapps.controller;

import com.rentalapps.exception.ApplicationException;
import com.rentalapps.exception.DatabaseException;
import com.rentalapps.service.ArchitectureService;
import com.rentalapps.vo.ArchitectureAuditRecord;
import com.rentalapps.vo.ArchitectureEventRequest;
import com.rentalapps.vo.ArchitectureOverviewResponse;
import com.rentalapps.vo.ArchitecturePublishResponse;
import com.rentalapps.vo.CustomerBean;
import java.io.IOException;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** REST APIs that expose the local Kafka-style architecture dashboard. */
@RestController
@RequestMapping("/api/architecture")
public class ArchitectureController {

  private final ArchitectureService architectureService;

  public ArchitectureController(ArchitectureService architectureService) {
    this.architectureService = architectureService;
  }

  @GetMapping("/overview")
  public ResponseEntity<ArchitectureOverviewResponse> overview() throws DatabaseException {
    return ResponseEntity.ok(architectureService.overview());
  }

  @PostMapping("/events")
  public ResponseEntity<ArchitecturePublishResponse> publish(@RequestBody ArchitectureEventRequest request)
      throws IOException, ApplicationException {
    return ResponseEntity.ok(architectureService.publish(request));
  }

  @GetMapping("/display/{region}")
  public ResponseEntity<List<CustomerBean>> display(@PathVariable String region,
                                                    @RequestParam(required = false) String locationId)
      throws IOException, DatabaseException {
    return ResponseEntity.ok(architectureService.display(region, locationId));
  }

  @GetMapping("/audit")
  public ResponseEntity<List<ArchitectureAuditRecord>> audit(@RequestParam(defaultValue = "25") int limit) {
    return ResponseEntity.ok(architectureService.auditEvents(limit));
  }
}
