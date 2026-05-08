package com.apidoc.controller;

import com.apidoc.dto.TestCaseDTO;
import com.apidoc.entity.TestCase;
import com.apidoc.service.TestCaseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/testcases")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class TestCaseController {

    private final TestCaseService testCaseService;
    private final ObjectMapper objectMapper;

    @GetMapping("/endpoint/{endpointId}")
    public ResponseEntity<List<TestCaseDTO>> listByEndpoint(@PathVariable Long endpointId) {
        log.info("Fetching test cases for endpoint: {}", endpointId);
        try {
            List<TestCaseDTO> testCases = testCaseService.getTestCasesByEndpointId(endpointId);
            return ResponseEntity.ok(testCases);
        } catch (Exception e) {
            log.error("Error fetching test cases: {}", e.getMessage(), e);
            throw e;
        }
    }

    @DeleteMapping("/endpoint/{endpointId}")
    public ResponseEntity<Void> deleteByEndpoint(@PathVariable Long endpointId) {
        log.info("Deleting test cases for endpoint: {}", endpointId);
        try {
            testCaseService.deleteByEndpointId(endpointId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error deleting test cases: {}", e.getMessage(), e);
            throw e;
        }
    }

    @PostMapping("/generate/{endpointId}")
    public ResponseEntity<List<TestCaseDTO>> generate(
            @PathVariable Long endpointId,
            @RequestBody(required = false) Map<String, Object> requestBody) {
        log.info("Generating test cases for endpoint: {}", endpointId);
        try {
            String testData = null;
            if (requestBody != null && requestBody.containsKey("testData")) {
                testData = objectMapper.writeValueAsString(requestBody.get("testData"));
            }
            List<TestCaseDTO> testCases = testCaseService.generateTestCases(
                    endpointId,
                    TestCase.TestType.INTEGRATION,
                    testData
            );
            return ResponseEntity.ok(testCases);
        } catch (Exception e) {
            log.error("Error generating test cases: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(null);
        }
    }

    @GetMapping("/export/curl/{endpointId}")
    public ResponseEntity<String> exportCurl(@PathVariable Long endpointId) {
        try {
            List<TestCaseDTO> testCases = testCaseService.getTestCasesByEndpointId(endpointId);
            if (testCases.isEmpty()) {
                return ResponseEntity.ok("# No test cases found");
            }
            StringBuilder curl = new StringBuilder();
            for (TestCaseDTO tc : testCases) {
                curl.append("# ").append(tc.getName()).append("\n");
                curl.append(tc.getCurlCommand()).append("\n\n");
            }
            return ResponseEntity.ok(curl.toString());
        } catch (Exception e) {
            log.error("Error exporting curl: {}", e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/export/postman/{endpointId}")
    public ResponseEntity<String> exportPostman(@PathVariable Long endpointId) {
        log.info("Exporting Postman collection for endpoint: {}", endpointId);
        try {
            String postmanCollection = testCaseService.generatePostmanCollectionForEndpoint(endpointId);
            return ResponseEntity.ok(postmanCollection);
        } catch (Exception e) {
            log.error("Error exporting Postman collection: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("{\"error\": \"导出Postman失败: " + e.getMessage() + "\"}");
        }
    }
}
