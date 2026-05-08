package com.apidoc.controller;

import com.apidoc.entity.ApiResponse;
import com.apidoc.repository.ApiResponseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/responses")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class ResponseController {

    private final ApiResponseRepository responseRepository;

    @GetMapping("/endpoint/{endpointId}")
    public ResponseEntity<List<ApiResponse>> listByEndpoint(@PathVariable Long endpointId) {
        return ResponseEntity.ok(responseRepository.findByEndpointId(endpointId));
    }

    @PostMapping
    public ResponseEntity<ApiResponse> create(@RequestBody Map<String, Object> request) {
        log.info("Creating response: {}", request);

        ApiResponse response = new ApiResponse();
        response.setStatusCode((String) request.get("statusCode"));
        response.setDescription((String) request.get("description"));
        response.setContentType((String) request.getOrDefault("contentType", "application/json"));
        response.setIsDefault((Boolean) request.getOrDefault("isDefault", false));

        if (request.get("headers") instanceof Map) {
            try {
                response.setHeaders(new com.fasterxml.jackson.databind.ObjectMapper()
                        .writeValueAsString(request.get("headers")));
            } catch (Exception e) {
                log.warn("Failed to serialize headers", e);
            }
        }

        if (request.get("examples") instanceof Map) {
            try {
                response.setExamples(new com.fasterxml.jackson.databind.ObjectMapper()
                        .writeValueAsString(request.get("examples")));
            } catch (Exception e) {
                log.warn("Failed to serialize examples", e);
            }
        }

        return ResponseEntity.ok(responseRepository.save(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse> update(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        return responseRepository.findById(id)
                .map(existing -> {
                    if (request.get("statusCode") != null) {
                        existing.setStatusCode((String) request.get("statusCode"));
                    }
                    if (request.get("description") != null) {
                        existing.setDescription((String) request.get("description"));
                    }
                    if (request.get("contentType") != null) {
                        existing.setContentType((String) request.get("contentType"));
                    }
                    if (request.get("isDefault") != null) {
                        existing.setIsDefault((Boolean) request.get("isDefault"));
                    }
                    return ResponseEntity.ok(responseRepository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        responseRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
