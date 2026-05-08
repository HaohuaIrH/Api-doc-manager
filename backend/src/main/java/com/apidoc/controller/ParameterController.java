package com.apidoc.controller;

import com.apidoc.entity.ApiEndpoint;
import com.apidoc.entity.ApiParameter;
import com.apidoc.repository.ApiEndpointRepository;
import com.apidoc.repository.ApiParameterRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/parameters")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class ParameterController {

    private final ApiParameterRepository parameterRepository;
    private final ApiEndpointRepository endpointRepository;
    private final ObjectMapper objectMapper;

    @GetMapping("/endpoint/{endpointId}")
    public ResponseEntity<List<ApiParameter>> listByEndpoint(@PathVariable Long endpointId) {
        return ResponseEntity.ok(parameterRepository.findByEndpointId(endpointId));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> request) {
        try {
            Long endpointId = Long.valueOf(request.get("endpointId").toString());
            ApiEndpoint endpoint = endpointRepository.findById(endpointId)
                    .orElseThrow(() -> new RuntimeException("Endpoint not found: " + endpointId));

            ApiParameter param = new ApiParameter();
            param.setEndpoint(endpoint);

            String locationStr = (String) request.getOrDefault("type", request.get("location"));
            if (locationStr != null) {
                try {
                    param.setLocation(ApiParameter.ParameterLocation.valueOf(locationStr));
                } catch (IllegalArgumentException e) {
                    param.setLocation(ApiParameter.ParameterLocation.QUERY);
                }
            }

            param.setName((String) request.get("name"));
            param.setDescription((String) request.get("description"));
            param.setRequired((Boolean) request.getOrDefault("required", false));
            param.setDataType((String) request.getOrDefault("dataType", "string"));
            param.setFormat((String) request.get("format"));
            param.setDefaultValue((String) request.get("defaultValue"));
            param.setExample((String) request.get("example"));

            if (request.get("minLength") != null) {
                param.setMinLength(((Number) request.get("minLength")).intValue());
            }
            if (request.get("maxLength") != null) {
                param.setMaxLength(((Number) request.get("maxLength")).intValue());
            }
            if (request.get("minimum") != null) {
                param.setMinimum(new BigDecimal(request.get("minimum").toString()));
            }
            if (request.get("maximum") != null) {
                param.setMaximum(new BigDecimal(request.get("maximum").toString()));
            }

            if (request.get("enumValues") instanceof List) {
                param.setEnumValues(toJson(request.get("enumValues")));
            }

            return ResponseEntity.ok(parameterRepository.save(param));
        } catch (Exception e) {
            log.error("Error creating parameter: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        return parameterRepository.findById(id)
                .map(existing -> {
                    if (request.get("type") != null || request.get("location") != null) {
                        String locationStr = (String) request.getOrDefault("type", request.get("location"));
                        if (locationStr != null) {
                            try {
                                existing.setLocation(ApiParameter.ParameterLocation.valueOf(locationStr));
                            } catch (IllegalArgumentException e) {
                                // ignore
                            }
                        }
                    }
                    if (request.get("name") != null) existing.setName((String) request.get("name"));
                    if (request.get("description") != null) existing.setDescription((String) request.get("description"));
                    if (request.get("required") != null) existing.setRequired((Boolean) request.get("required"));
                    if (request.get("dataType") != null) existing.setDataType((String) request.get("dataType"));
                    if (request.get("format") != null) existing.setFormat((String) request.get("format"));
                    if (request.get("defaultValue") != null) existing.setDefaultValue((String) request.get("defaultValue"));
                    if (request.get("example") != null) existing.setExample((String) request.get("example"));

                    return ResponseEntity.ok(parameterRepository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        parameterRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }
}
