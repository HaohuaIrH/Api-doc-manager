package com.apidoc.controller;

import com.apidoc.dto.ProjectDTO;
import com.apidoc.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    public ResponseEntity<List<ProjectDTO>> list() {
        List<ProjectDTO> projects = projectService.findAccessibleProjects();
        return ResponseEntity.ok(projects);
    }

    @GetMapping("/my")
    public ResponseEntity<List<ProjectDTO>> listMyProjects() {
        return ResponseEntity.ok(projectService.findMyProjects());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectDTO> get(@PathVariable Long id) {
        ProjectDTO project = projectService.findById(id);
        if (project == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(project);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody ProjectDTO project) {
        try {
            ProjectDTO created = projectService.create(project);
            return ResponseEntity.ok(created);
        } catch (IllegalStateException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "User not authenticated");
            return ResponseEntity.status(401).body(error);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody ProjectDTO project) {
        ProjectDTO updated = projectService.update(id, project);
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        boolean deleted = projectService.delete(id);
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}/permanent")
    public ResponseEntity<Void> deletePermanently(@PathVariable Long id) {
        boolean deleted = projectService.deletePermanently(id);
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/batch-delete")
    public ResponseEntity<?> batchDelete(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "IDs list is required and cannot be empty");
            return ResponseEntity.badRequest().body(error);
        }

        Map<String, Object> result = projectService.batchDelete(ids);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/batch-delete/permanent")
    public ResponseEntity<?> batchDeletePermanently(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "IDs list is required and cannot be empty");
            return ResponseEntity.badRequest().body(error);
        }

        Map<String, Object> result = projectService.batchDeletePermanently(ids);
        return ResponseEntity.ok(result);
    }
}
