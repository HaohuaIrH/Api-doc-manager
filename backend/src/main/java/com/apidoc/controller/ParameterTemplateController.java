package com.apidoc.controller;

import com.apidoc.dto.ParameterTemplateDTO;
import com.apidoc.service.ParameterTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/parameter-templates")
@RequiredArgsConstructor
public class ParameterTemplateController {

    private final ParameterTemplateService templateService;

    @GetMapping
    public ResponseEntity<List<ParameterTemplateDTO>> getAll(@RequestParam Long documentId) {
        return ResponseEntity.ok(templateService.getAllByDocumentId(documentId));
    }

    @GetMapping("/folders")
    public ResponseEntity<List<String>> getFolders(@RequestParam Long documentId) {
        return ResponseEntity.ok(templateService.getFoldersByDocumentId(documentId));
    }

    @GetMapping("/folder/{folderName}")
    public ResponseEntity<List<ParameterTemplateDTO>> getByFolder(
            @PathVariable String folderName,
            @RequestParam Long documentId) {
        return ResponseEntity.ok(templateService.getTemplatesByFolderName(folderName, documentId));
    }

    @PostMapping
    public ResponseEntity<ParameterTemplateDTO> create(@RequestBody ParameterTemplateDTO dto) {
        return ResponseEntity.ok(templateService.create(dto));
    }

    @PostMapping("/from-endpoint/{endpointId}")
    public ResponseEntity<ParameterTemplateDTO> createFromEndpoint(@PathVariable Long endpointId) {
        return ResponseEntity.ok(templateService.createFromEndpoint(endpointId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        templateService.delete(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/folder/{folderName}")
    public ResponseEntity<Void> deleteByFolder(
            @PathVariable String folderName,
            @RequestParam Long documentId) {
        templateService.deleteByFolderName(folderName, documentId);
        return ResponseEntity.ok().build();
    }
}
