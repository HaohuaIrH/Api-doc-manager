package com.apidoc.service;

import com.apidoc.dto.GlobalParameterDTO;
import com.apidoc.entity.GlobalParameter;
import com.apidoc.entity.User;
import com.apidoc.repository.GlobalParameterRepository;
import com.apidoc.repository.UserRepository;
import com.apidoc.security.SecurityContextHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * GlobalParameter 链路属主校验单元测试
 */
@ExtendWith(MockitoExtension.class)
class GlobalParameterServiceOwnershipTest {

    @Mock
    private GlobalParameterRepository parameterRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GlobalParameterService service;

    private User owner;
    private User other;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(1L).username("owner").enabled(true).role(User.UserRole.USER).build();
        other = User.builder().id(2L).username("other").enabled(true).role(User.UserRole.USER).build();
        loginAs(other);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHelper.clear();
    }

    private void loginAs(User user) {
        SecurityContextHelper.setCurrentUserId(user.getId());
        SecurityContextHelper.setCurrentUser(user);
        org.mockito.Mockito.lenient().when(userRepository.findById(user.getId()))
                .thenReturn(Optional.of(user));
    }

    @Test
    void updateByNonOwnerDenied() {
        GlobalParameter parameter = GlobalParameter.builder()
                .id(5L).name("token").owner(owner).build();
        when(parameterRepository.findById(5L)).thenReturn(Optional.of(parameter));

        assertThatThrownBy(() -> service.update(5L, new GlobalParameterDTO()))
                .isInstanceOf(AccessDeniedException.class);
        verify(parameterRepository, never()).save(any());
    }

    @Test
    void deleteByNonOwnerDenied() {
        GlobalParameter parameter = GlobalParameter.builder()
                .id(5L).name("token").owner(owner).build();
        when(parameterRepository.findById(5L)).thenReturn(Optional.of(parameter));

        assertThatThrownBy(() -> service.delete(5L))
                .isInstanceOf(AccessDeniedException.class);
        verify(parameterRepository, never()).delete(any(GlobalParameter.class));
    }

    @Test
    void deleteByOwnerAllowed() {
        loginAs(owner);
        GlobalParameter parameter = GlobalParameter.builder()
                .id(5L).name("token").owner(owner).build();
        when(parameterRepository.findById(5L)).thenReturn(Optional.of(parameter));

        service.delete(5L);

        verify(parameterRepository).delete(parameter);
    }

    @Test
    void createAssignsOwnerToCurrentUser() {
        GlobalParameter saved = GlobalParameter.builder()
                .id(9L).name("newParam").owner(other).build();
        when(parameterRepository.findByName("newParam")).thenReturn(Optional.empty());
        when(parameterRepository.save(any(GlobalParameter.class))).thenReturn(saved);

        GlobalParameterDTO dto = GlobalParameterDTO.builder().name("newParam").build();
        GlobalParameterDTO result = service.create(dto);

        assertThat(result.getId()).isEqualTo(9L);
        org.mockito.ArgumentCaptor<GlobalParameter> captor =
                org.mockito.ArgumentCaptor.forClass(GlobalParameter.class);
        verify(parameterRepository).save(captor.capture());
        assertThat(captor.getValue().getOwner()).isNotNull();
        assertThat(captor.getValue().getOwner().getId()).isEqualTo(2L);
    }

    @Test
    void createChildUnderForeignParameterDenied() {
        GlobalParameter parent = GlobalParameter.builder()
                .id(7L).name("parent").owner(owner).build();
        when(parameterRepository.findByName("child")).thenReturn(Optional.empty());
        when(parameterRepository.findById(7L)).thenReturn(Optional.of(parent));

        GlobalParameterDTO dto = GlobalParameterDTO.builder().name("child").parentId(7L).build();

        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(AccessDeniedException.class);
        verify(parameterRepository, never()).save(any());
    }

    @Test
    void createWithoutAuthenticationDenied() {
        SecurityContextHelper.clear();

        GlobalParameterDTO dto = GlobalParameterDTO.builder().name("newParam").build();

        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(org.springframework.security.authentication.AuthenticationCredentialsNotFoundException.class);
    }
}
