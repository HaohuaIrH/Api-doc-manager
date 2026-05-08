package com.apidoc.service;

import com.apidoc.dto.ParameterTemplateDTO;
import com.apidoc.entity.ApiEndpoint;
import com.apidoc.entity.ParameterTemplate;
import com.apidoc.repository.ApiEndpointRepository;
import com.apidoc.repository.ParameterTemplateRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ParameterTemplateService {

    private final ParameterTemplateRepository templateRepository;
    private final ApiEndpointRepository endpointRepository;
    private final ObjectMapper objectMapper;

    public List<ParameterTemplateDTO> getAllByDocumentId(Long documentId) {
        if (documentId == null) {
            return Collections.emptyList();
        }
        List<ParameterTemplate> templates = templateRepository.findAllByDocumentIdOrderByFolderName(documentId);
        return templates.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<String> getFoldersByDocumentId(Long documentId) {
        if (documentId == null) {
            return Collections.emptyList();
        }
        return templateRepository.findDistinctFolderNamesByDocumentId(documentId);
    }

    public List<ParameterTemplateDTO> getTemplatesByFolderName(String folderName, Long documentId) {
        if (folderName == null || folderName.isEmpty() || documentId == null) {
            return Collections.emptyList();
        }
        List<ParameterTemplate> templates = templateRepository.findByFolderNameAndDocumentId(folderName, documentId);
        return templates.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public ParameterTemplateDTO create(ParameterTemplateDTO dto) {
        if (dto == null) {
            throw new RuntimeException("Template data cannot be null");
        }
        if (dto.getFolderName() == null || dto.getFolderName().trim().isEmpty()) {
            throw new RuntimeException("Folder name cannot be empty");
        }
        if (dto.getDocumentId() == null) {
            throw new RuntimeException("Document ID cannot be null");
        }

        ParameterTemplate template = dto.toEntity();
        
        String paramsJson;
        try {
            paramsJson = objectMapper.writeValueAsString(dto.getParameters() != null ? dto.getParameters() : Collections.emptyList());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize parameters", e);
        }
        template.setParameters(paramsJson);
        
        template = templateRepository.save(template);
        return toDTO(template);
    }

    public ParameterTemplateDTO createFromEndpoint(Long endpointId) {
        ApiEndpoint endpoint = endpointRepository.findById(endpointId)
                .orElseThrow(() -> new RuntimeException("Endpoint not found: " + endpointId));

        String folderName = endpoint.getSummary() != null ? endpoint.getSummary() : endpoint.getPath();
        
        String paramsJson;
        try {
            paramsJson = objectMapper.writeValueAsString(Collections.emptyList());
            if (endpoint.getParameters() != null && !endpoint.getParameters().isEmpty()) {
                List<Map<String, Object>> params = endpoint.getParameters().stream()
                        .map(p -> {
                            Map<String, Object> map = new HashMap<>();
                            map.put("name", p.getName());
                            map.put("location", p.getLocation() != null ? p.getLocation().name() : "QUERY");
                            map.put("dataType", p.getDataType());
                            map.put("description", p.getDescription());
                            map.put("required", p.getRequired());
                            map.put("example", p.getExample());
                            return map;
                        })
                        .collect(Collectors.toList());
                paramsJson = objectMapper.writeValueAsString(params);
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize parameters", e);
        }

        ParameterTemplate template = ParameterTemplate.builder()
                .folderName(folderName)
                .templateName(folderName + " 参数模板")
                .parameters(paramsJson)
                .documentId(endpoint.getDocument().getId())
                .tenantId(1L)
                .build();

        template = templateRepository.save(template);
        return toDTO(template);
    }

    public void delete(Long id) {
        if (!templateRepository.existsById(id)) {
            throw new RuntimeException("Template not found: " + id);
        }
        templateRepository.deleteById(id);
    }

    public void deleteByFolderName(String folderName, Long documentId) {
        List<ParameterTemplate> templates = templateRepository.findByFolderNameAndDocumentId(folderName, documentId);
        if (templates.isEmpty()) {
            throw new RuntimeException("Folder not found: " + folderName);
        }
        templateRepository.deleteAll(templates);
    }

    private ParameterTemplateDTO toDTO(ParameterTemplate template) {
        List<Map<String, Object>> params = Collections.emptyList();
        if (template.getParameters() != null && !template.getParameters().isEmpty()) {
            try {
                params = objectMapper.readValue(
                        template.getParameters(),
                        new TypeReference<List<Map<String, Object>>>() {}
                );
            } catch (JsonProcessingException e) {
                params = Collections.emptyList();
            }
        }
        return ParameterTemplateDTO.fromEntity(template, params);
    }
}
