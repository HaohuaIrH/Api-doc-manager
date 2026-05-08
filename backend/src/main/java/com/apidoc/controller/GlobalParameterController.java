package com.apidoc.controller;

import com.apidoc.dto.GlobalParameterDTO;
import com.apidoc.service.GlobalParameterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/global-parameters")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "全局参数管理", description = "全局参数表管理接口")
public class GlobalParameterController {

    private final GlobalParameterService parameterService;

    @GetMapping
    @Operation(summary = "获取所有参数", description = "获取所有全局参数，包含树形结构")
    public ResponseEntity<List<GlobalParameterDTO>> getAllParameters() {
        return ResponseEntity.ok(parameterService.getAllParameters());
    }

    @GetMapping("/root")
    @Operation(summary = "获取顶级参数", description = "获取所有顶级参数（父参数为空的参数）")
    public ResponseEntity<List<GlobalParameterDTO>> getRootParameters() {
        return ResponseEntity.ok(parameterService.getRootParameters());
    }

    @GetMapping("/simple-types")
    @Operation(summary = "获取简单类型参数", description = "获取所有简单类型的全局参数")
    public ResponseEntity<List<GlobalParameterDTO>> getSimpleTypes() {
        return ResponseEntity.ok(parameterService.getSimpleTypes());
    }

    @GetMapping("/complex-types")
    @Operation(summary = "获取复杂类型参数", description = "获取所有复杂类型的全局参数")
    public ResponseEntity<List<GlobalParameterDTO>> getComplexTypes() {
        return ResponseEntity.ok(parameterService.getComplexTypes());
    }

    @GetMapping("/search")
    @Operation(summary = "搜索参数", description = "根据关键词搜索全局参数")
    public ResponseEntity<List<GlobalParameterDTO>> search(@RequestParam String q) {
        return ResponseEntity.ok(parameterService.search(q));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取单个参数", description = "根据ID获取单个全局参数详情")
    public ResponseEntity<GlobalParameterDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(parameterService.getById(id));
    }

    @PostMapping
    @Operation(summary = "创建参数", description = "创建新的全局参数")
    public ResponseEntity<GlobalParameterDTO> create(@RequestBody GlobalParameterDTO dto) {
        try {
            return ResponseEntity.ok(parameterService.create(dto));
        } catch (RuntimeException e) {
            log.error("创建参数失败", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新参数", description = "更新已存在的全局参数")
    public ResponseEntity<GlobalParameterDTO> update(@PathVariable Long id, @RequestBody GlobalParameterDTO dto) {
        try {
            return ResponseEntity.ok(parameterService.update(id, dto));
        } catch (RuntimeException e) {
            log.error("更新参数失败", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除参数", description = "删除指定的全局参数")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            parameterService.delete(id);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            log.error("删除参数失败", e);
            return ResponseEntity.badRequest().build();
        }
    }
}
