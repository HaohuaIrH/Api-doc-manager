package com.apidoc.controller;

import com.apidoc.dto.DocumentDTO;
import com.apidoc.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DocumentController {

    private final DocumentService documentService;

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<DocumentDTO>> listByProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(documentService.findByProjectId(projectId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentDTO> get(@PathVariable Long id) {
        DocumentDTO document = documentService.findById(id);
        if (document == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(document);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody DocumentDTO document) {
        try {
            if (document.getProjectId() == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Project ID is required");
                return ResponseEntity.badRequest().body(error);
            }

            if (document.getName() == null || document.getName().trim().isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Document name is required");
                return ResponseEntity.badRequest().body(error);
            }

            DocumentDTO created = documentService.create(document);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to create document: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody DocumentDTO document) {
        try {
            DocumentDTO updated = documentService.update(id, document);
            if (updated == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to update document: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            boolean deleted = documentService.delete(id);
            if (!deleted) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}/permanent")
    public ResponseEntity<Void> deletePermanently(@PathVariable Long id) {
        try {
            boolean deleted = documentService.deletePermanently(id);
            if (!deleted) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/batch-delete")
    public ResponseEntity<?> batchDelete(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "IDs list is required and cannot be empty");
            return ResponseEntity.badRequest().body(error);
        }

        try {
            Map<String, Object> result = documentService.batchDelete(ids);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to batch delete documents: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @DeleteMapping("/batch-delete/permanent")
    public ResponseEntity<?> batchDeletePermanently(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "IDs list is required and cannot be empty");
            return ResponseEntity.badRequest().body(error);
        }

        try {
            Map<String, Object> result = documentService.batchDeletePermanently(ids);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to batch permanently delete documents: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
