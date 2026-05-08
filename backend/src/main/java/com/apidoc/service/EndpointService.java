package com.apidoc.service;

import com.apidoc.dto.EndpointDTO;
import com.apidoc.entity.ApiDocument;
import com.apidoc.entity.ApiEndpoint;
import com.apidoc.entity.ApiParameter;
import com.apidoc.repository.ApiDocumentRepository;
import com.apidoc.repository.ApiEndpointRepository;
import com.apidoc.repository.ApiParameterRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EndpointService {

    private final ApiEndpointRepository endpointRepository;
    private final ApiDocumentRepository documentRepository;
    private final ApiParameterRepository parameterRepository;
    private final ObjectMapper objectMapper;

    public List<EndpointDTO> findByDocumentId(Long documentId) {
        List<ApiEndpoint> endpoints = endpointRepository.findByDocumentId(documentId);
        return endpoints.stream()
                .map(this::toDTOWithParams)
                .collect(Collectors.toList());
    }

    public EndpointDTO findById(Long id) {
        return endpointRepository.findById(id)
                .map(this::toDTOWithParams)
                .orElse(null);
    }

    @Transactional
    public EndpointDTO create(Map<String, Object> request) {
        if (request.get("documentId") == null) {
            throw new RuntimeException("documentId is required");
        }
        Long documentId = Long.valueOf(request.get("documentId").toString());
        ApiDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));

        String path = (String) request.get("path");
        String methodStr = (String) request.get("method");

        if (path == null || path.trim().isEmpty()) {
            throw new RuntimeException("path is required");
        }
        if (methodStr == null) {
            throw new RuntimeException("method is required");
        }

        ApiEndpoint.HttpMethod method;
        try {
            method = ApiEndpoint.HttpMethod.valueOf(methodStr);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid HTTP method: " + methodStr);
        }

        var existing = endpointRepository.findByDocumentIdAndPathAndMethod(documentId, path, method);
        if (existing.isPresent()) {
            throw new RuntimeException("该文档下已存在相同路径和方法的端点: " + method + " " + path);
        }

        ApiEndpoint endpoint = new ApiEndpoint();
        endpoint.setDocument(document);
        endpoint.setPath(path);
        endpoint.setMethod(method);
        endpoint.setSummary((String) request.get("summary"));
        endpoint.setDescription((String) request.get("description"));
        endpoint.setDeprecated(request.get("deprecated") != null ? (Boolean) request.get("deprecated") : false);

        endpoint = endpointRepository.save(endpoint);

        // 保存参数
        saveParameters(endpoint, request);

        return toDTOWithParams(endpoint);
    }

    @Transactional
    public EndpointDTO update(Long id, Map<String, Object> request) {
        ApiEndpoint endpoint = endpointRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Endpoint not found: " + id));

        if (request.get("path") != null) {
            endpoint.setPath((String) request.get("path"));
        }
        if (request.get("method") != null) {
            endpoint.setMethod(ApiEndpoint.HttpMethod.valueOf((String) request.get("method")));
        }
        if (request.get("summary") != null) {
            endpoint.setSummary((String) request.get("summary"));
        }
        if (request.get("description") != null) {
            endpoint.setDescription((String) request.get("description"));
        }
        if (request.get("deprecated") != null) {
            endpoint.setDeprecated((Boolean) request.get("deprecated"));
        }

        endpoint = endpointRepository.save(endpoint);

        // 更新参数
        if (request.containsKey("parameters")) {
            saveParameters(endpoint, request);
        }

        return toDTOWithParams(endpoint);
    }

    @Transactional
    public void delete(Long id) {
        endpointRepository.deleteById(id);
    }

    /**
     * 即时测试端点 - 直接返回测试配置，不生成TestCase
     */
    public Map<String, Object> testEndpoint(Long id, Map<String, Object> params) {
        ApiEndpoint endpoint = endpointRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Endpoint not found: " + id));

        log.info("Testing endpoint: {} {} with params: {}", endpoint.getMethod(), endpoint.getPath(), params);

        // 构建请求配置
        Map<String, Object> requestConfig = new java.util.HashMap<>();
        requestConfig.put("url", endpoint.getPath());
        requestConfig.put("method", endpoint.getMethod().name());
        requestConfig.put("params", params);

        // 获取参数定义
        List<ApiParameter> parameters = parameterRepository.findByEndpointId(id);
        Map<String, Object> paramDefinitions = new java.util.HashMap<>();
        for (ApiParameter param : parameters) {
            Map<String, Object> def = new java.util.HashMap<>();
            def.put("type", param.getDataType());
            def.put("required", param.getRequired());
            def.put("location", param.getLocation() != null ? param.getLocation().name() : "QUERY");
            paramDefinitions.put(param.getName(), def);
        }

        // 构建测试结果
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("endpointId", id);
        result.put("path", endpoint.getPath());
        result.put("method", endpoint.getMethod().name());
        result.put("requestConfig", requestConfig);
        result.put("paramDefinitions", paramDefinitions);
        result.put("userParams", params);
        result.put("status", "READY");
        result.put("message", "测试配置已生成，可以使用生成的代码进行实际调用");

        // 生成cURL命令
        String curl = generateCurlCommand(endpoint, params);
        result.put("curlCommand", curl);

        return result;
    }

    private String generateCurlCommand(ApiEndpoint endpoint, Map<String, Object> params) {
        StringBuilder curl = new StringBuilder();
        curl.append("curl -X ").append(endpoint.getMethod()).append(" \\\n");
        curl.append("  -H \"Content-Type: application/json\" \\\n");
        
        if (params != null && !params.isEmpty()) {
            try {
                String jsonBody = objectMapper.writeValueAsString(params);
                curl.append("  -d '" ).append(jsonBody).append("' \\\n");
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize params to JSON", e);
            }
        }
        
        curl.append("  \"http://localhost:8081").append(endpoint.getPath()).append("\"");
        return curl.toString();
    }

    @SuppressWarnings("unchecked")
    private void saveParameters(ApiEndpoint endpoint, Map<String, Object> request) {
        // 删除旧的参数
        parameterRepository.deleteByEndpointId(endpoint.getId());

        Object paramsObj = request.get("parameters");
        if (paramsObj == null) {
            return;
        }

        List<Map<String, Object>> parameters;
        if (paramsObj instanceof List) {
            parameters = (List<Map<String, Object>>) paramsObj;
        } else {
            return;
        }

        for (Map<String, Object> paramData : parameters) {
            ApiParameter param = new ApiParameter();
            param.setEndpoint(endpoint);

            String locationStr = (String) paramData.get("location");
            if (locationStr != null) {
                try {
                    param.setLocation(ApiParameter.ParameterLocation.valueOf(locationStr));
                } catch (IllegalArgumentException e) {
                    param.setLocation(ApiParameter.ParameterLocation.QUERY);
                }
            } else {
                param.setLocation(ApiParameter.ParameterLocation.QUERY);
            }

            param.setName((String) paramData.get("name"));
            param.setDescription((String) paramData.get("description"));
            param.setRequired(paramData.get("required") != null ? (Boolean) paramData.get("required") : false);
            param.setDataType((String) paramData.get("dataType"));
            param.setFormat((String) paramData.get("format"));
            param.setDefaultValue((String) paramData.get("defaultValue"));
            param.setExample((String) paramData.get("example"));

            if (paramData.get("minLength") != null) {
                param.setMinLength(((Number) paramData.get("minLength")).intValue());
            }
            if (paramData.get("maxLength") != null) {
                param.setMaxLength(((Number) paramData.get("maxLength")).intValue());
            }
            if (paramData.get("minimum") != null) {
                param.setMinimum(new java.math.BigDecimal(paramData.get("minimum").toString()));
            }
            if (paramData.get("maximum") != null) {
                param.setMaximum(new java.math.BigDecimal(paramData.get("maximum").toString()));
            }

            if (paramData.get("enumValues") instanceof List) {
                try {
                    param.setEnumValues(objectMapper.writeValueAsString(paramData.get("enumValues")));
                } catch (JsonProcessingException e) {
                    log.warn("Failed to serialize enumValues", e);
                }
            }

            parameterRepository.save(param);
        }
    }

    private EndpointDTO toDTOWithParams(ApiEndpoint endpoint) {
        EndpointDTO dto = EndpointDTO.fromEntity(endpoint);

        List<ApiParameter> params = parameterRepository.findByEndpointId(endpoint.getId());
        if (params != null && !params.isEmpty()) {
            List<Map<String, Object>> paramList = params.stream()
                    .map(this::paramToMap)
                    .collect(Collectors.toList());
            dto.setParameters(paramList);
        }

        return dto;
    }

    private Map<String, Object> paramToMap(ApiParameter param) {
        Map<String, Object> map = new java.util.HashMap<>();
        map.put("id", param.getId() != null ? param.getId() : 0L);
        map.put("location", param.getLocation() != null ? param.getLocation().name() : "QUERY");
        map.put("name", param.getName() != null ? param.getName() : "");
        map.put("description", param.getDescription() != null ? param.getDescription() : "");
        map.put("required", param.getRequired() != null ? param.getRequired() : false);
        map.put("dataType", param.getDataType() != null ? param.getDataType() : "string");
        map.put("format", param.getFormat() != null ? param.getFormat() : "");
        map.put("defaultValue", param.getDefaultValue() != null ? param.getDefaultValue() : "");
        map.put("example", param.getExample() != null ? param.getExample() : "");
        map.put("minLength", param.getMinLength() != null ? param.getMinLength() : 0);
        map.put("maxLength", param.getMaxLength() != null ? param.getMaxLength() : 0);
        map.put("minimum", param.getMinimum() != null ? param.getMinimum() : null);
        map.put("maximum", param.getMaximum() != null ? param.getMaximum() : null);
        map.put("enumValues", param.getEnumValues() != null ? param.getEnumValues() : "");
        return map;
    }
}
