package com.apidoc.service;

import com.apidoc.dto.TestCaseDTO;
import com.apidoc.entity.ApiEndpoint;
import com.apidoc.entity.ApiParameter;
import com.apidoc.entity.Project;
import com.apidoc.entity.TestCase;
import com.apidoc.repository.ApiEndpointRepository;
import com.apidoc.repository.ApiParameterRepository;
import com.apidoc.repository.TestCaseRepository;
import com.apidoc.security.SecurityContextHelper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 测试用例服务 - 自动生成测试用例，支持用户数据隔离
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TestCaseService {

    private final TestCaseRepository testCaseRepository;
    private final ApiEndpointRepository endpointRepository;
    private final ApiParameterRepository parameterRepository;
    private final ObjectMapper objectMapper;

    /**
     * 删除端点的所有测试用例
     */
    public void deleteByEndpointId(Long endpointId) {
        Long currentUserId = SecurityContextHelper.getCurrentUserId();
        if (currentUserId == null) {
            throw new SecurityException("User not authenticated");
        }
        
        // 验证端点访问权限
        if (!hasEndpointAccess(endpointId, currentUserId)) {
            throw new SecurityException("No permission to access this endpoint");
        }
        
        testCaseRepository.deleteByEndpointId(endpointId);
        log.info("已删除端点 {} 的所有测试用例", endpointId);
    }

    /**
     * 根据端点自动生成测试用例（需验证端点访问权限）
     */
    public List<TestCaseDTO> generateTestCases(Long endpointId, TestCase.TestType type, String testData) {
        Long currentUserId = SecurityContextHelper.getCurrentUserId();
        if (currentUserId == null) {
            throw new SecurityException("User not authenticated");
        }

        // 验证端点访问权限
        if (!hasEndpointAccess(endpointId, currentUserId)) {
            throw new SecurityException("No permission to access this endpoint");
        }

        ApiEndpoint endpoint = endpointRepository.findById(endpointId)
                .orElseThrow(() -> new RuntimeException("端点不存在: " + endpointId));

        // 删除该端点的所有现有测试用例
        testCaseRepository.deleteByEndpointId(endpointId);
        log.info("已删除端点 {} 的所有现有测试用例", endpointId);

        List<TestCaseDTO> generatedCases = new ArrayList<>();

        // 1. 生成正常用例 - 必填参数有效值
        TestCaseDTO normalCase = generateNormalCase(endpoint, type, testData);
        TestCase savedNormal = testCaseRepository.save(toEntity(normalCase, endpoint));
        generatedCases.add(TestCaseDTO.fromEntity(savedNormal));

        // 2. 生成边界值测试
        TestCaseDTO boundaryCase = generateBoundaryCase(endpoint, type, testData);
        TestCase savedBoundary = testCaseRepository.save(toEntity(boundaryCase, endpoint));
        generatedCases.add(TestCaseDTO.fromEntity(savedBoundary));

        // 3. 生成异常测试 - 缺少必填参数
        TestCaseDTO missingRequiredCase = generateMissingRequiredCase(endpoint, type);
        TestCase savedMissing = testCaseRepository.save(toEntity(missingRequiredCase, endpoint));
        generatedCases.add(TestCaseDTO.fromEntity(savedMissing));

        // 4. 生成异常测试 - 参数格式错误
        TestCaseDTO invalidFormatCase = generateInvalidFormatCase(endpoint, type);
        TestCase savedInvalid = testCaseRepository.save(toEntity(invalidFormatCase, endpoint));
        generatedCases.add(TestCaseDTO.fromEntity(savedInvalid));

        log.info("为端点 {} 生成了 {} 个测试用例", endpointId, generatedCases.size());
        return generatedCases.stream().map(this::enrichWithCode).collect(Collectors.toList());
    }

    /**
     * 获取测试用例列表（需验证端点访问权限）
     */
    @Transactional(readOnly = true)
    public List<TestCaseDTO> getTestCasesByEndpointId(Long endpointId) {
        Long currentUserId = SecurityContextHelper.getCurrentUserId();
        if (currentUserId == null) {
            return List.of();
        }

        // 验证端点访问权限
        if (!hasEndpointAccess(endpointId, currentUserId)) {
            return List.of();
        }

        return testCaseRepository.findByEndpointId(endpointId).stream()
                .map(TestCaseDTO::fromEntity)
                .map(this::enrichWithCode)
                .collect(Collectors.toList());
    }

    /**
     * 检查用户是否有权访问端点
     */
    private boolean hasEndpointAccess(Long endpointId, Long userId) {
        return endpointRepository.findById(endpointId)
                .map(endpoint -> {
                    // 获取项目
                    if (endpoint.getDocument() == null || endpoint.getDocument().getProject() == null) {
                        return false;
                    }
                    Project project = endpoint.getDocument().getProject();
                    // 项目所有者
                    if (project.getOwner() != null && project.getOwner().getId().equals(userId)) {
                        return true;
                    }
                    // 公开项目
                    if (project.getVisibility() == Project.Visibility.PUBLIC) {
                        return true;
                    }
                    return false;
                })
                .orElse(false);
    }

    /**
     * 生成正常用例
     */
    private TestCaseDTO generateNormalCase(ApiEndpoint endpoint, TestCase.TestType type, String testData) {
        Map<String, Object> requestConfig = new HashMap<>();
        requestConfig.put("url", endpoint.getPath());
        requestConfig.put("method", endpoint.getMethod().name());

        // 解析用户传入的测试数据
        Map<String, Object> userTestData = new HashMap<>();
        if (testData != null && !testData.isEmpty()) {
            try {
                userTestData = objectMapper.readValue(testData, Map.class);
                log.info("用户传入的测试数据: {}", userTestData);
            } catch (Exception e) {
                log.warn("解析测试数据失败: {}", e.getMessage());
            }
        }

        // 检测是否为注册/创建类接口（需要唯一标识符）
        boolean isRegisterOrCreateApi = endpoint.getPath().toLowerCase().contains("register") ||
                endpoint.getPath().toLowerCase().contains("create") ||
                endpoint.getPath().toLowerCase().contains("signup");

        // 收集所有参数，优先使用用户传入的值
        Map<String, Object> params = new HashMap<>();
        List<ApiParameter> parameters = parameterRepository.findByEndpointId(endpoint.getId());
        for (ApiParameter param : parameters) {
            Object value;
            // 优先使用用户传入的测试数据
            if (userTestData.containsKey(param.getName())) {
                value = userTestData.get(param.getName());
                
                // 如果是注册/创建类接口且是用户名字段，自动添加时间戳确保唯一性
                if (isRegisterOrCreateApi && value != null &&
                    (param.getName().equalsIgnoreCase("username") ||
                     param.getName().equalsIgnoreCase("email") ||
                     param.getName().equalsIgnoreCase("loginname") ||
                     param.getName().equalsIgnoreCase("account"))) {
                    
                    String uniqueValue = value.toString() + "_" + System.currentTimeMillis();
                    params.put(param.getName(), uniqueValue);
                    log.info("注册接口 - 自动添加时间戳确保唯一性: {} = {}", param.getName(), uniqueValue);
                    continue;
                }
                
                log.info("使用用户传入的参数值: {} = {}", param.getName(), value);
            } else if (Boolean.TRUE.equals(param.getRequired())) {
                // 如果用户没有提供，使用示例值或默认值
                value = parseExample(param.getExample());
                if (value == null) {
                    value = getDefaultValue(param.getDataType());
                }
                
                // 如果是注册/创建类接口且是用户名字段，自动添加时间戳
                if (isRegisterOrCreateApi && value != null &&
                    (param.getName().equalsIgnoreCase("username") ||
                     param.getName().equalsIgnoreCase("email") ||
                     param.getName().equalsIgnoreCase("loginname") ||
                     param.getName().equalsIgnoreCase("account"))) {
                    
                    String uniqueValue = value.toString() + "_" + System.currentTimeMillis();
                    params.put(param.getName(), uniqueValue);
                    log.info("注册接口 - 自动添加时间戳确保唯一性: {} = {}", param.getName(), uniqueValue);
                    continue;
                }
            } else {
                // 非必填参数也使用用户数据或示例值
                value = userTestData.get(param.getName());
                if (value == null) {
                    value = parseExample(param.getExample());
                }
            }
            if (value != null) {
                params.put(param.getName(), value);
            }
        }

        if (!params.isEmpty()) {
            requestConfig.put(getRequestBodyLocation(parameters), params);
        }

        // 设置期望响应
        Map<String, Object> expected = new HashMap<>();
        expected.put("statusCode", 200);

        return TestCaseDTO.builder()
                .endpointId(endpoint.getId())
                .endpointPath(endpoint.getPath())
                .endpointMethod(endpoint.getMethod().name())
                .name(endpoint.getSummary() + " - 正常场景")
                .description("测试" + endpoint.getMethod() + " " + endpoint.getPath() + "接口正常调用")
                .type(type)
                .priority(TestCase.TestPriority.HIGH)
                .requestConfig(toJson(requestConfig))
                .expectedResponse(toJson(expected))
                .testData(testData)
                .enabled(true)
                .build();
    }

    /**
     * 生成边界值测试
     */
    private TestCaseDTO generateBoundaryCase(ApiEndpoint endpoint, TestCase.TestType type, String testData) {
        Map<String, Object> requestConfig = new HashMap<>();
        requestConfig.put("url", endpoint.getPath());
        requestConfig.put("method", endpoint.getMethod().name());

        List<ApiParameter> parameters = parameterRepository.findByEndpointId(endpoint.getId());
        Map<String, Object> params = new HashMap<>();

        for (ApiParameter param : parameters) {
            if (Boolean.TRUE.equals(param.getRequired()) || parseExample(param.getExample()) != null) {
                // 如果有最大长度限制，使用最大长度
                if (param.getMaxLength() != null && param.getMaxLength() > 0) {
                    params.put(param.getName(), generateString(param.getMaxLength()));
                    log.info("边界值测试 - 使用最大长度 {}: {}", param.getName(), param.getMaxLength());
                }
                // 如果有最大数值限制
                else if (param.getMaximum() != null) {
                    params.put(param.getName(), param.getMaximum());
                    log.info("边界值测试 - 使用最大值 {}: {}", param.getName(), param.getMaximum());
                }
                // 如果没有任何约束，使用默认值50个字符
                else if ("string".equalsIgnoreCase(param.getDataType())) {
                    params.put(param.getName(), generateString(50));
                    log.info("边界值测试 - 使用默认值长度 50: {}", param.getName());
                }
                // 数字类型使用默认值
                else if ("integer".equalsIgnoreCase(param.getDataType()) || "number".equalsIgnoreCase(param.getDataType())) {
                    params.put(param.getName(), 1000000);
                    log.info("边界值测试 - 使用默认数字值 1000000: {}", param.getName());
                }
                // 其他类型使用示例值
                else {
                    Object exampleValue = parseExample(param.getExample());
                    if (exampleValue != null) {
                        params.put(param.getName(), exampleValue);
                    }
                }
            }
        }

        if (!params.isEmpty()) {
            requestConfig.put(getRequestBodyLocation(parameters), params);
        }

        Map<String, Object> expected = new HashMap<>();
        expected.put("statusCode", 200);

        return TestCaseDTO.builder()
                .endpointId(endpoint.getId())
                .endpointPath(endpoint.getPath())
                .endpointMethod(endpoint.getMethod().name())
                .name(endpoint.getSummary() + " - 边界值测试")
                .description("测试边界值参数")
                .type(type)
                .priority(TestCase.TestPriority.MEDIUM)
                .requestConfig(toJson(requestConfig))
                .expectedResponse(toJson(expected))
                .testData(testData)
                .enabled(true)
                .build();
    }

    /**
     * 生成缺少必填参数测试
     */
    private TestCaseDTO generateMissingRequiredCase(ApiEndpoint endpoint, TestCase.TestType type) {
        Map<String, Object> requestConfig = new HashMap<>();
        requestConfig.put("url", endpoint.getPath());
        requestConfig.put("method", endpoint.getMethod().name());

        List<ApiParameter> parameters = parameterRepository.findByEndpointId(endpoint.getId());
        List<String> requiredParams = parameters.stream()
                .filter(p -> Boolean.TRUE.equals(p.getRequired()))
                .map(ApiParameter::getName)
                .collect(Collectors.toList());

        Map<String, Object> expected = new HashMap<>();
        expected.put("statusCode", 400);

        return TestCaseDTO.builder()
                .endpointId(endpoint.getId())
                .endpointPath(endpoint.getPath())
                .endpointMethod(endpoint.getMethod().name())
                .name(endpoint.getSummary() + " - 缺少必填参数")
                .description("测试缺少必填参数 " + requiredParams + " 时的响应")
                .type(type)
                .priority(TestCase.TestPriority.HIGH)
                .requestConfig(toJson(requestConfig))
                .expectedResponse(toJson(expected))
                .testData(toJson(Map.of("missingParams", requiredParams)))
                .enabled(true)
                .build();
    }

    /**
     * 生成参数格式错误测试
     */
    private TestCaseDTO generateInvalidFormatCase(ApiEndpoint endpoint, TestCase.TestType type) {
        Map<String, Object> requestConfig = new HashMap<>();
        requestConfig.put("url", endpoint.getPath());
        requestConfig.put("method", endpoint.getMethod().name());

        List<ApiParameter> parameters = parameterRepository.findByEndpointId(endpoint.getId());
        Map<String, Object> params = new HashMap<>();

        for (ApiParameter param : parameters) {
            if (Boolean.TRUE.equals(param.getRequired())) {
                params.put(param.getName(), getInvalidValue(param.getDataType()));
            }
        }

        if (!params.isEmpty()) {
            requestConfig.put(getRequestBodyLocation(parameters), params);
        }

        Map<String, Object> expected = new HashMap<>();
        expected.put("statusCode", 400);

        return TestCaseDTO.builder()
                .endpointId(endpoint.getId())
                .endpointPath(endpoint.getPath())
                .endpointMethod(endpoint.getMethod().name())
                .name(endpoint.getSummary() + " - 参数格式错误")
                .description("测试参数格式错误时的响应")
                .type(type)
                .priority(TestCase.TestPriority.MEDIUM)
                .requestConfig(toJson(requestConfig))
                .expectedResponse(toJson(expected))
                .enabled(true)
                .build();
    }

    /**
     * 转换为实体
     */
    private TestCase toEntity(TestCaseDTO dto, ApiEndpoint endpoint) {
        return TestCase.builder()
                .endpoint(endpoint)
                .name(dto.getName())
                .description(dto.getDescription())
                .type(dto.getType())
                .priority(dto.getPriority())
                .requestConfig(dto.getRequestConfig())
                .expectedResponse(dto.getExpectedResponse())
                .testData(dto.getTestData())
                .enabled(dto.getEnabled())
                .build();
    }

    /**
     * 丰富代码示例
     */
    private TestCaseDTO enrichWithCode(TestCaseDTO dto) {
        try {
            log.debug("Enriching test case: name={}, endpointPath={}, endpointMethod={}",
                    dto.getName(), dto.getEndpointPath(), dto.getEndpointMethod());

            String curl = generateCurl(dto.getRequestConfig());
            dto.setCurlCommand(curl);

            String java = generateJavaCode(dto);
            dto.setJavaCode(java);

            String js = generateJavascriptCode(dto);
            dto.setJavascriptCode(js);

            String python = generatePythonCode(dto);
            dto.setPythonCode(python);

            String go = generateGoCode(dto);
            dto.setGoCode(go);

            return dto;
        } catch (Exception e) {
            log.error("EnrichWithCode error for test case: {}", dto.getName(), e);
            dto.setCurlCommand("# Code generation error: " + e.getMessage());
            dto.setJavaCode("// Code generation error: " + e.getMessage());
            dto.setJavascriptCode("// Code generation error: " + e.getMessage());
            dto.setPythonCode("# Code generation error: " + e.getMessage());
            dto.setGoCode("// Code generation error: " + e.getMessage());
            return dto;
        }
    }

    /**
     * 生成cURL命令
     */
    public String generateCurl(String requestConfig) {
        try {
            log.debug("generateCurl input: {}", requestConfig);
            Map<String, Object> config = objectMapper.readValue(requestConfig, Map.class);
            StringBuilder curl = new StringBuilder("curl -X ");
            curl.append(config.get("method"));

            // 添加头信息
            curl.append(" \\\n  -H \"Content-Type: application/json\"");

            // 添加请求体
            if (config.containsKey("body")) {
                Map<String, Object> body = (Map<String, Object>) config.get("body");
                String jsonBody = objectMapper.writeValueAsString(body);
                curl.append(" \\\n  -d '").append(jsonBody).append("'");
            }

            curl.append(" \\\n  \"").append(config.get("url")).append("\"");

            return curl.toString();
        } catch (Exception e) {
            log.error("生成cURL失败", e);
            return "# 生成失败: " + e.getMessage();
        }
    }

    /**
     * 生成Java代码
     */
    private String generateJavaCode(TestCaseDTO dto) {
        log.debug("generateJavaCode - name={}, endpointPath={}, endpointMethod={}",
                dto.getName(), dto.getEndpointPath(), dto.getEndpointMethod());
        try {
            String template = """
// Java - HttpClient 示例
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class %s {
    public static void main(String[] args) throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        String json = "{\\"userId\\": 1}"; // 根据实际参数调整

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8081%s"))
                .method("%s", HttpRequest.BodyPublishers.ofString(json))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());

        System.out.println("Status: " + response.statusCode());
        System.out.println("Body: " + response.body());
    }
}
""";
            return String.format(template, dto.getName(), dto.getEndpointPath(), dto.getEndpointMethod());
        } catch (Exception e) {
            log.error("generateJavaCode failed: name={}, endpointPath={}, endpointMethod={}, error={}",
                    dto.getName(), dto.getEndpointPath(), dto.getEndpointMethod(), e.getMessage(), e);
            throw new RuntimeException("generateJavaCode failed: " + e.getMessage(), e);
        }
    }

    /**
     * 生成JavaScript代码
     */
    private String generateJavascriptCode(TestCaseDTO dto) {
        try {
            return String.format("""
// JavaScript - Fetch API 示例
async function %s() {
    try {
        const response = await fetch('http://localhost:8081%s', {
            method: '%s',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                // 根据实际参数调整
            })
        });

        const data = await response.json();
        console.log('Status:', response.status);
        console.log('Response:', data);

        return data;
    } catch (error) {
        console.error('Error:', error);
        throw error;
    }
}

%s();
""", dto.getName().replaceAll("\\s+", ""), dto.getEndpointPath(),
                dto.getEndpointMethod(), dto.getName().replaceAll("\\s+", ""));
        } catch (Exception e) {
            log.error("generateJavascriptCode failed: {}", e.getMessage(), e);
            return "// Code generation failed: " + e.getMessage();
        }
    }

    /**
     * 生成Python代码
     */
    private String generatePythonCode(TestCaseDTO dto) {
        try {
            return String.format("""
// Python - requests 示例
import requests
import json

def %s():
    url = "http://localhost:8081%s"

    payload = {
        # 根据实际参数调整
    }

    headers = {
        'Content-Type': 'application/json'
    }

    response = requests.%s(url, headers=headers, json=payload)

    print(f"Status: {response.status_code}")
    print(f"Response: {response.json()}")

if __name__ == "__main__":
    %s()
""", dto.getName().replaceAll("\\s+", "_"), dto.getEndpointPath(),
                dto.getEndpointMethod().toLowerCase(), dto.getName().replaceAll("\\s+", "_"));
        } catch (Exception e) {
            log.error("generatePythonCode failed: {}", e.getMessage(), e);
            return "# Code generation failed: " + e.getMessage();
        }
    }

    /**
     * 生成Go代码
     */
    private String generateGoCode(TestCaseDTO dto) {
        try {
            return String.format("""
// Go - net/http 示例
package main

import (
    "bytes"
    "encoding/json"
    "fmt"
    "net/http"
)

func %s() error {
    url := "http://localhost:8081%s"

    payload := map[string]interface{}{
        // 根据实际参数调整
    }

    jsonData, err := json.Marshal(payload)
    if err != nil {
        return err
    }

    req, err := http.NewRequest("%s", url, bytes.NewBuffer(jsonData))
    if err != nil {
        return err
    }

    req.Header.Set("Content-Type", "application/json")

    client := &http.Client{}
    resp, err := client.Do(req)
    if err != nil {
        return err
    }
    defer resp.Body.Close()

    fmt.Printf("Status: %d\\n", resp.StatusCode)
    return nil
}

func main() {
    %s()
}
""", dto.getName().replaceAll("\\s+", ""), dto.getEndpointPath(),
                dto.getEndpointMethod(), dto.getName().replaceAll("\\s+", ""));
        } catch (Exception e) {
            log.error("generateGoCode failed: {}", e.getMessage(), e);
            return "// Code generation failed: " + e.getMessage();
        }
    }

    /**
     * 生成Postman Collection
     */
    public String generatePostmanCollection(Long documentId, Long projectId) {
        List<ApiEndpoint> endpoints = endpointRepository.findByDocumentId(documentId);
        Map<String, Object> collection = new HashMap<>();
        collection.put("info", Map.of(
                "name", "API Documentation Collection",
                "schema", "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
        ));

        List<Map<String, Object>> items = new ArrayList<>();
        for (ApiEndpoint endpoint : endpoints) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", endpoint.getSummary());
            item.put("request", Map.of(
                    "method", endpoint.getMethod().name(),
                    "header", new ArrayList<>(),
                    "url", Map.of("raw", "{{baseUrl}}" + endpoint.getPath())
            ));
            items.add(item);
        }
        collection.put("item", items);

        try {
            return objectMapper.writeValueAsString(collection);
        } catch (Exception e) {
            log.error("生成Postman Collection失败", e);
            return "{}";
        }
    }

    /**
     * 根据端点ID生成Postman Collection
     */
    public String generatePostmanCollectionForEndpoint(Long endpointId) {
        log.info("为端点 {} 生成Postman Collection", endpointId);

        ApiEndpoint endpoint = endpointRepository.findById(endpointId)
                .orElseThrow(() -> new RuntimeException("Endpoint not found: " + endpointId));

        if (endpoint.getDocument() == null) {
            throw new RuntimeException("Document not found for endpoint: " + endpointId);
        }

        Long documentId = endpoint.getDocument().getId();
        Long projectId = endpoint.getDocument().getProject() != null ?
                endpoint.getDocument().getProject().getId() : null;

        List<ApiEndpoint> endpoints = endpointRepository.findByDocumentId(documentId);
        Map<String, Object> collection = new HashMap<>();

        String projectName = endpoint.getDocument().getProject() != null ?
                endpoint.getDocument().getProject().getName() : "API Collection";
        String documentName = endpoint.getDocument().getName() != null ?
                endpoint.getDocument().getName() : "API Documentation";

        collection.put("info", Map.of(
                "name", projectName + " - " + documentName,
                "schema", "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
        ));

        List<Map<String, Object>> items = new ArrayList<>();
        for (ApiEndpoint ep : endpoints) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", ep.getSummary() != null ? ep.getSummary() : ep.getPath());
            item.put("description", ep.getDescription() != null ? ep.getDescription() : "");

            Map<String, Object> request = new HashMap<>();
            request.put("method", ep.getMethod().name());

            String baseUrl = ep.getDocument() != null && ep.getDocument().getProject() != null &&
                    ep.getDocument().getProject().getBaseUrl() != null ?
                    ep.getDocument().getProject().getBaseUrl() : "http://localhost:8081";
            request.put("url", Map.of("raw", baseUrl + ep.getPath()));

            List<Map<String, Object>> headers = new ArrayList<>();
            headers.add(Map.of(
                    "key", "Content-Type",
                    "value", "application/json",
                    "type", "text"
            ));
            request.put("header", headers);

            item.put("request", request);
            items.add(item);
        }
        collection.put("item", items);

        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(collection);
        } catch (Exception e) {
            log.error("生成Postman Collection失败", e);
            throw new RuntimeException("生成Postman Collection失败: " + e.getMessage());
        }
    }

    // ==================== 辅助方法 ====================

    private String getRequestBodyLocation(List<ApiParameter> parameters) {
        for (ApiParameter param : parameters) {
            if (param.getLocation() == ApiParameter.ParameterLocation.REQUEST_BODY) {
                return "body";
            }
        }
        return "query";
    }

    private Object parseExample(String example) {
        if (example == null || example.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(example, Object.class);
        } catch (Exception e) {
            return example;
        }
    }

    private Object getDefaultValue(String dataType) {
        if (dataType == null) return "value";
        switch (dataType.toLowerCase()) {
            case "string": return "string";
            case "integer":
            case "int": return 0;
            case "long": return 0L;
            case "boolean": return true;
            case "array": return new ArrayList<>();
            case "object": return new HashMap<>();
            default: return "value";
        }
    }

    private Object getInvalidValue(String dataType) {
        if (dataType == null) return "@#$%";
        switch (dataType.toLowerCase()) {
            case "string": return "@#$%invalid";
            case "integer":
            case "int":
            case "long": return -999999;
            case "boolean": return "invalid";
            default: return "@#$%";
        }
    }

    private String generateString(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append("a");
        }
        return sb.toString();
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }
}
