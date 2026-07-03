package com.rentalapps.controller;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@RestController
@RequestMapping("/api/chroma")
public class ChromaAdminController {

  private final RestClient chromaClient;

  public ChromaAdminController(@Value("${chroma.url:${CHROMA_URL:http://localhost:8000}}") String chromaUrl) {
    this.chromaClient = RestClient.builder()
        .baseUrl(chromaUrl)
        .build();
  }

  @GetMapping("/heartbeat")
  public ResponseEntity<String> heartbeat() {
    return ResponseEntity.ok(chromaClient.get()
        .uri("/api/v2/heartbeat")
        .retrieve()
        .body(String.class));
  }

  @GetMapping("/identity")
  public ResponseEntity<String> identity() {
    return getJson("/api/v2/auth/identity");
  }

  @GetMapping("/tenants/{tenant}/databases/{database}/collections")
  public ResponseEntity<String> collections(@PathVariable String tenant, @PathVariable String database) {
    return getJson("/api/v2/tenants/{tenant}/databases/{database}/collections", tenant, database);
  }

  @PostMapping("/tenants/{tenant}/databases/{database}/collections")
  public ResponseEntity<String> createCollection(@PathVariable String tenant,
                                                 @PathVariable String database,
                                                 @RequestBody Map<String, Object> payload) {
    return postJson("/api/v2/tenants/{tenant}/databases/{database}/collections", payload, tenant, database);
  }

  @GetMapping("/tenants/{tenant}/databases/{database}/collections/{collectionId}/count")
  public ResponseEntity<String> count(@PathVariable String tenant,
                                      @PathVariable String database,
                                      @PathVariable String collectionId) {
    return getJson("/api/v2/tenants/{tenant}/databases/{database}/collections/{collectionId}/count",
        tenant, database, collectionId);
  }

  @PostMapping("/tenants/{tenant}/databases/{database}/collections/{collectionId}/get")
  public ResponseEntity<String> records(@PathVariable String tenant,
                                        @PathVariable String database,
                                        @PathVariable String collectionId,
                                        @RequestBody Map<String, Object> payload) {
    return postJson("/api/v2/tenants/{tenant}/databases/{database}/collections/{collectionId}/get",
        payload, tenant, database, collectionId);
  }

  private ResponseEntity<String> getJson(String uri, Object... variables) {
    String body = chromaClient.get()
        .uri(uri, variables)
        .retrieve()
        .body(String.class);
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .body(body);
  }

  private ResponseEntity<String> postJson(String uri, Map<String, Object> payload, Object... variables) {
    String body = chromaClient.post()
        .uri(uri, variables)
        .contentType(MediaType.APPLICATION_JSON)
        .body(payload)
        .retrieve()
        .body(String.class);
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .body(body);
  }
}
