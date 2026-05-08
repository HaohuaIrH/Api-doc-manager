package com.apidoc.dto;

import com.apidoc.entity.GlobalParameter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlobalParameterDTO {

    private Long id;

    private String name;

    private GlobalParameter.ParameterType dataType;

    private String exampleValue;

    private String description;

    private Long parentId;

    private Integer sortOrder;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Builder.Default
    private List<GlobalParameterDTO> children = new ArrayList<>();

    private boolean isComplexType() {
        return this.dataType == GlobalParameter.ParameterType.OBJECT ||
               this.dataType == GlobalParameter.ParameterType.ARRAY;
    }

    private boolean isSimpleType() {
        return !isComplexType();
    }
}
