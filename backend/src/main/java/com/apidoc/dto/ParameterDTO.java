package com.apidoc.dto;

import com.apidoc.entity.ApiParameter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 参数定义DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParameterDTO {

    private Long id;
    private Long endpointId;
    private ApiParameter.ParameterLocation location;
    private String name;
    private String description;
    private Boolean required;
    private String dataType;
    private String format;
    private String defaultValue;
    private String example;
    private String schemaDef;
    private String enumValues;
    private String validationRules;
    private Integer minLength;
    private Integer maxLength;
    private BigDecimal minimum;
    private BigDecimal maximum;
    private String pattern;
    private Integer sortOrder;

    public static ParameterDTO fromEntity(ApiParameter entity) {
        return ParameterDTO.builder()
                .id(entity.getId())
                .endpointId(entity.getEndpoint().getId())
                .location(entity.getLocation())
                .name(entity.getName())
                .description(entity.getDescription())
                .required(entity.getRequired())
                .dataType(entity.getDataType())
                .format(entity.getFormat())
                .defaultValue(entity.getDefaultValue())
                .example(entity.getExample())
                .schemaDef(entity.getSchemaDef())
                .enumValues(entity.getEnumValues())
                .validationRules(entity.getValidationRules())
                .minLength(entity.getMinLength())
                .maxLength(entity.getMaxLength())
                .minimum(entity.getMinimum())
                .maximum(entity.getMaximum())
                .pattern(entity.getPattern())
                .sortOrder(entity.getSortOrder())
                .build();
    }

    public ApiParameter toEntity() {
        return ApiParameter.builder()
                .location(this.location)
                .name(this.name)
                .description(this.description)
                .required(this.required)
                .dataType(this.dataType)
                .format(this.format)
                .defaultValue(this.defaultValue)
                .example(this.example)
                .schemaDef(this.schemaDef)
                .enumValues(this.enumValues)
                .validationRules(this.validationRules)
                .minLength(this.minLength)
                .maxLength(this.maxLength)
                .minimum(this.minimum)
                .maximum(this.maximum)
                .pattern(this.pattern)
                .sortOrder(this.sortOrder)
                .build();
    }
}
