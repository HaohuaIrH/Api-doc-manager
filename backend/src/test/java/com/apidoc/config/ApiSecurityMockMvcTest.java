package com.apidoc.config;

import com.apidoc.controller.ParameterController;
import com.apidoc.entity.ApiDocument;
import com.apidoc.entity.ApiEndpoint;
import com.apidoc.entity.ApiParameter;
import com.apidoc.entity.Project;
import com.apidoc.entity.User;
import com.apidoc.repository.ApiDocumentRepository;
import com.apidoc.repository.ApiEndpointRepository;
import com.apidoc.repository.ApiParameterRepository;
import com.apidoc.repository.ApiResponseRepository;
import com.apidoc.repository.ProjectRepository;
import com.apidoc.repository.UserRepository;
import com.apidoc.security.AccessControlService;
import com.apidoc.service.JwtService;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 业务 API 认证与属主校验的验收测试：
 * - 不带 token / 无效 token 访问业务 API -> 401
 * - 非 owner 修改他人资源 -> 403
 * - owner 正常操作 -> 200
 */
@WebMvcTest(controllers = {ParameterController.class, ApiSecurityMockMvcTest.ProbeController.class})
@Import({SecurityConfig.class, AccessControlService.class})
class ApiSecurityMockMvcTest {

    private static final Long OWNER_ID = 1L;
    private static final Long OTHER_ID = 2L;
    private static final Long PROJECT_ID = 10L;
    private static final Long DOCUMENT_ID = 100L;
    private static final Long ENDPOINT_ID = 1000L;
    private static final Long PARAMETER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtService jwtService;
    @MockBean
    private UserRepository userRepository;
    @MockBean
    private ProjectRepository projectRepository;
    @MockBean
    private ApiDocumentRepository documentRepository;
    @MockBean
    private ApiEndpointRepository endpointRepository;
    @MockBean
    private ApiParameterRepository parameterRepository;
    @MockBean
    private ApiResponseRepository responseRepository;

    private User owner;
    private User other;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(OWNER_ID).username("owner").enabled(true)
                .role(User.UserRole.USER).build();
        other = User.builder().id(OTHER_ID).username("other").enabled(true)
                .role(User.UserRole.USER).build();

        User disabled = User.builder().id(3L).username("disabled").enabled(false)
                .role(User.UserRole.USER).build();

        Project project = Project.builder().id(PROJECT_ID).name("project")
                .owner(owner).visibility(Project.Visibility.PRIVATE).build();
        ApiDocument document = ApiDocument.builder().id(DOCUMENT_ID)
                .project(project).name("document").build();
        ApiEndpoint endpoint = ApiEndpoint.builder().id(ENDPOINT_ID)
                .document(document).path("/pets").method(ApiEndpoint.HttpMethod.GET).build();
        ApiParameter parameter = ApiParameter.builder().id(PARAMETER_ID)
                .endpoint(endpoint).name("page").location(ApiParameter.ParameterLocation.QUERY).build();

        when(jwtService.extractUsername("owner-token")).thenReturn("owner");
        when(jwtService.extractUsername("other-token")).thenReturn("other");
        when(jwtService.extractUsername("disabled-token")).thenReturn("disabled");
        when(jwtService.extractUsername("bad-token")).thenThrow(new JwtException("malformed token"));

        when(userRepository.findByUsername("owner")).thenReturn(Optional.of(owner));
        when(userRepository.findByUsername("other")).thenReturn(Optional.of(other));
        when(userRepository.findByUsername("disabled")).thenReturn(Optional.of(disabled));

        when(parameterRepository.findById(PARAMETER_ID)).thenReturn(Optional.of(parameter));
        when(endpointRepository.findById(ENDPOINT_ID)).thenReturn(Optional.of(endpoint));
        when(parameterRepository.findByEndpointId(ENDPOINT_ID)).thenReturn(List.of(parameter));
    }

    @Test
    void businessApiWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/probe"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authEndpointStaysPublic() throws Exception {
        // /api/auth/** 放行：请求到达 MVC 层（无对应 Controller 返回 404，而不是 401/403）
        mockMvc.perform(get("/api/auth/nonexistent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteParameterWithoutTokenReturns401() throws Exception {
        mockMvc.perform(delete("/api/parameters/" + PARAMETER_ID))
                .andExpect(status().isUnauthorized());
        verify(parameterRepository, org.mockito.Mockito.never()).deleteById(PARAMETER_ID);
    }

    @Test
    void deleteParameterWithInvalidTokenReturns401() throws Exception {
        mockMvc.perform(delete("/api/parameters/" + PARAMETER_ID)
                        .header("Authorization", "Bearer bad-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteParameterWithDisabledUserTokenReturns401() throws Exception {
        mockMvc.perform(delete("/api/parameters/" + PARAMETER_ID)
                        .header("Authorization", "Bearer disabled-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteParameterByNonOwnerReturns403() throws Exception {
        mockMvc.perform(delete("/api/parameters/" + PARAMETER_ID)
                        .header("Authorization", "Bearer other-token"))
                .andExpect(status().isForbidden());
        verify(parameterRepository, org.mockito.Mockito.never()).deleteById(PARAMETER_ID);
    }

    @Test
    void deleteParameterByOwnerReturns200() throws Exception {
        mockMvc.perform(delete("/api/parameters/" + PARAMETER_ID)
                        .header("Authorization", "Bearer owner-token"))
                .andExpect(status().isOk());
        verify(parameterRepository).deleteById(PARAMETER_ID);
    }

    @Test
    void parameterListHiddenFromNonOwnerOfPrivateProject() throws Exception {
        mockMvc.perform(get("/api/parameters/endpoint/" + ENDPOINT_ID)
                        .header("Authorization", "Bearer other-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void parameterListVisibleToOwner() throws Exception {
        mockMvc.perform(get("/api/parameters/endpoint/" + ENDPOINT_ID)
                        .header("Authorization", "Bearer owner-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(PARAMETER_ID.intValue()));
    }

    @Test
    void parameterListWithoutTokenIsEmpty() throws Exception {
        mockMvc.perform(get("/api/parameters/endpoint/" + ENDPOINT_ID))
                .andExpect(status().isUnauthorized());
    }

    /**
     * 探针控制器：验证默认 authenticated() 规则覆盖所有业务 API
     */
    @RestController
    static class ProbeController {
        @GetMapping("/api/probe")
        public String probe() {
            return "ok";
        }
    }
}
