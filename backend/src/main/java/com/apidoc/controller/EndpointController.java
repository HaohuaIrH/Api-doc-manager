package com.apidoc.controller;

import com.apidoc.dto.EndpointDTO;
import com.apidoc.service.EndpointService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 接口端点控制器
 */
@RestController
@RequestMapping("/api/endpoints")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class EndpointController {

    private final EndpointService endpointService;

    /**
     * 根据文档获取所有端点
     */
    @GetMapping("/document/{documentId}")
    public ResponseEntity<List<EndpointDTO>> listByDocument(@PathVariable Long documentId) {
        log.info("Fetching endpoints for documentId: {}", documentId);
        try {
            List<EndpointDTO> endpoints = endpointService.findByDocumentId(documentId);
            log.info("Found {} endpoints", endpoints.size());
            return ResponseEntity.ok(endpoints);
        } catch (Exception e) {
            log.error("Error fetching endpoints: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 获取单个端点
     */
    @GetMapping("/{id}")
    public ResponseEntity<EndpointDTO> get(@PathVariable Long id) {
        EndpointDTO endpoint = endpointService.findById(id);
        if (endpoint == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(endpoint);
    }

    /**
     * 创建端点
     */
    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> request) {
        try {
            EndpointDTO created = endpointService.create(request);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            log.error("Error creating endpoint: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 更新端点
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        try {
            EndpointDTO updated = endpointService.update(id, request);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("Error updating endpoint: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 删除端点
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        endpointService.delete(id);
        return ResponseEntity.ok().build();
    }

    /**
     * 即时测试接口 - 直接调用API，不生成TestCase
     */
    @PostMapping("/{id}/test")
    public ResponseEntity<?> testEndpoint(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        log.info("Testing endpoint {} with params: {}", id, params);
        try {
            Map<String, Object> result = endpointService.testEndpoint(id, params);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error testing endpoint: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
