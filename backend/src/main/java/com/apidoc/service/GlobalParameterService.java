package com.apidoc.service;

import com.apidoc.dto.GlobalParameterDTO;
import com.apidoc.entity.GlobalParameter;
import com.apidoc.entity.User;
import com.apidoc.repository.GlobalParameterRepository;
import com.apidoc.repository.UserRepository;
import com.apidoc.security.SecurityContextHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GlobalParameterService {

    private final GlobalParameterRepository parameterRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<GlobalParameterDTO> getAllParameters() {
        if (!isAuthenticated()) {
            return List.of();
        }
        List<GlobalParameter> parameters = parameterRepository.findAllWithNestedChildren();
        return parameters.stream()
                .map(this::toDTOWithChildren)
                .collect(Collectors.toList());
    }

    public List<GlobalParameterDTO> getRootParameters() {
        if (!isAuthenticated()) {
            return List.of();
        }
        List<GlobalParameter> parameters = parameterRepository.findByParentIsNull();
        return parameters.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<GlobalParameterDTO> getSimpleTypes() {
        if (!isAuthenticated()) {
            return List.of();
        }
        List<GlobalParameter> parameters = parameterRepository.findAllSimpleTypes();
        return parameters.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<GlobalParameterDTO> getComplexTypes() {
        if (!isAuthenticated()) {
            return List.of();
        }
        List<GlobalParameter> parameters = parameterRepository.findAllComplexTypes();
        return parameters.stream()
                .map(this::toDTOWithChildren)
                .collect(Collectors.toList());
    }

    public List<GlobalParameterDTO> search(String keyword) {
        if (!isAuthenticated()) {
            return List.of();
        }
        List<GlobalParameter> parameters = parameterRepository.searchByName(keyword);
        return parameters.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public GlobalParameterDTO getById(Long id) {
        requireCurrentUser();

        GlobalParameter parameter = parameterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("参数不存在: " + id));
        return toDTOWithChildren(parameter);
    }

    public GlobalParameterDTO create(GlobalParameterDTO dto) {
        User owner = requireCurrentUser();

        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new RuntimeException("参数名称不能为空");
        }

        if (parameterRepository.findByName(dto.getName()).isPresent()) {
            throw new RuntimeException("参数名称已存在: " + dto.getName());
        }

        // 将字符串转换为枚举类型
        GlobalParameter.ParameterType paramType = convertToParameterType(dto.getDataType());

        GlobalParameter parameter = GlobalParameter.builder()
                .name(dto.getName())
                .dataType(paramType)
                .exampleValue(dto.getExampleValue())
                .description(dto.getDescription())
                .sortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0)
                .owner(owner)
                .build();

        if (dto.getParentId() != null) {
            GlobalParameter parent = parameterRepository.findById(dto.getParentId())
                    .orElseThrow(() -> new RuntimeException("父参数不存在: " + dto.getParentId()));
            // 不能在他人属主的参数下挂子参数
            requireParameterOwner(parent, owner);
            parameter.setParent(parent);
        }

        parameter = parameterRepository.save(parameter);
        return toDTO(parameter);
    }

    public GlobalParameterDTO update(Long id, GlobalParameterDTO dto) {
        User currentUser = requireCurrentUser();

        GlobalParameter parameter = parameterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("参数不存在: " + id));

        // 仅属主可修改（历史遗留的无属主参数保持原行为，任何已认证用户可管理）
        requireParameterOwner(parameter, currentUser);

        if (dto.getName() != null && !dto.getName().equals(parameter.getName())) {
            if (parameterRepository.findByName(dto.getName()).isPresent()) {
                throw new RuntimeException("参数名称已存在: " + dto.getName());
            }
            parameter.setName(dto.getName());
        }

        if (dto.getDataType() != null) {
            parameter.setDataType(convertToParameterType(dto.getDataType()));
        }

        if (dto.getExampleValue() != null) {
            parameter.setExampleValue(dto.getExampleValue());
        }

        if (dto.getDescription() != null) {
            parameter.setDescription(dto.getDescription());
        }

        if (dto.getSortOrder() != null) {
            parameter.setSortOrder(dto.getSortOrder());
        }

        parameter = parameterRepository.save(parameter);
        return toDTO(parameter);
    }

    public void delete(Long id) {
        User currentUser = requireCurrentUser();

        GlobalParameter parameter = parameterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("参数不存在: " + id));

        // 仅属主可删除（历史遗留的无属主参数保持原行为，任何已认证用户可管理）
        requireParameterOwner(parameter, currentUser);

        parameterRepository.delete(parameter);
    }

    private User requireCurrentUser() {
        Long currentUserId = SecurityContextHelper.getCurrentUserId();
        if (currentUserId == null) {
            throw new AuthenticationCredentialsNotFoundException("User not authenticated");
        }
        return userRepository.findById(currentUserId)
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException("User not found: " + currentUserId));
    }

    private boolean isAuthenticated() {
        return SecurityContextHelper.getCurrentUserId() != null;
    }

    /**
     * 校验参数属主：无属主的历史参数保持原行为；有属主时仅属主本人可操作
     */
    private void requireParameterOwner(GlobalParameter parameter, User currentUser) {
        if (parameter.getOwner() != null
                && parameter.getOwner().getId() != null
                && !parameter.getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("只有属主才能修改该全局参数: " + parameter.getName());
        }
    }

    private GlobalParameterDTO toDTO(GlobalParameter parameter) {
        return GlobalParameterDTO.builder()
                .id(parameter.getId())
                .name(parameter.getName())
                .dataType(parameter.getDataType())
                .exampleValue(parameter.getExampleValue())
                .description(parameter.getDescription())
                .parentId(parameter.getParent() != null ? parameter.getParent().getId() : null)
                .sortOrder(parameter.getSortOrder())
                .createdAt(parameter.getCreatedAt())
                .updatedAt(parameter.getUpdatedAt())
                .build();
    }

    private GlobalParameterDTO toDTOWithChildren(GlobalParameter parameter) {
        GlobalParameterDTO dto = toDTO(parameter);

        if (parameter.getChildren() != null && !parameter.getChildren().isEmpty()) {
            List<GlobalParameterDTO> children = parameter.getChildren().stream()
                    .sorted((a, b) -> a.getSortOrder().compareTo(b.getSortOrder()))
                    .map(this::toDTO)
                    .collect(Collectors.toList());
            dto.setChildren(children);
        }

        return dto;
    }

    private GlobalParameter.ParameterType convertToParameterType(Object dataType) {
        if (dataType == null) {
            return GlobalParameter.ParameterType.STRING;
        }

        if (dataType instanceof GlobalParameter.ParameterType) {
            return (GlobalParameter.ParameterType) dataType;
        }

        if (dataType instanceof String) {
            String typeStr = ((String) dataType).toUpperCase();
            try {
                return GlobalParameter.ParameterType.valueOf(typeStr);
            } catch (IllegalArgumentException e) {
                return GlobalParameter.ParameterType.STRING;
            }
        }

        return GlobalParameter.ParameterType.STRING;
    }
}
