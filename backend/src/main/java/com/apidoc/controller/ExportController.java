package com.apidoc.controller;

import com.apidoc.entity.ApiDocument;
import com.apidoc.export.LatexExporter;
import com.apidoc.export.MarkdownExporter;
import com.apidoc.repository.ApiDocumentRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "导出管理", description = "文档导出相关接口")
public class ExportController {

    private final MarkdownExporter markdownExporter;
    private final LatexExporter latexExporter;
    private final ApiDocumentRepository documentRepository;

    @GetMapping("/markdown/{documentId}")
    @Transactional(readOnly = true)
    @Operation(summary = "导出为Markdown", description = "将单个文档导出为Markdown格式")
    public ResponseEntity<String> exportToMarkdown(@PathVariable Long documentId) {
        try {
            // 使用 findById 而不是 findByIdWithDetails，避免懒加载问题
            ApiDocument document = documentRepository.findById(documentId)
                    .orElseThrow(() -> new RuntimeException("文档不存在: " + documentId));

            String content = markdownExporter.exportDocument(document, "1");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/markdown; charset=utf-8"));
            headers.setContentDispositionFormData("attachment",
                    document.getName() + ".md");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(content);
        } catch (Exception e) {
            log.error("导出Markdown失败 documentId={}", documentId, e);
            return ResponseEntity.internalServerError()
                    .body("导出失败: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }

    @GetMapping("/latex/{documentId}")
    @Transactional(readOnly = true)
    @Operation(summary = "导出为LaTeX", description = "将单个文档导出为LaTeX格式")
    public ResponseEntity<String> exportToLatex(@PathVariable Long documentId) {
        try {
            ApiDocument document = documentRepository.findById(documentId)
                    .orElseThrow(() -> new RuntimeException("文档不存在: " + documentId));

            String content = latexExporter.exportDocumentToLatex(document, 1);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/x-tex; charset=utf-8"));
            headers.setContentDispositionFormData("attachment",
                    document.getName() + ".tex");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(content);
        } catch (Exception e) {
            log.error("导出LaTeX失败 documentId={}", documentId, e);
            return ResponseEntity.internalServerError()
                    .body("导出失败: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }

    @GetMapping("/project/{projectId}/markdown")
    @Transactional(readOnly = true)
    @Operation(summary = "导出项目为Markdown", description = "将项目下所有文档导出为单个Markdown文件")
    public ResponseEntity<String> exportProjectToMarkdown(@PathVariable Long projectId) {
        try {
            String content = markdownExporter.exportProject(projectId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/markdown; charset=utf-8"));
            headers.setContentDispositionFormData("attachment",
                    "project_" + projectId + ".md");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(content);
        } catch (Exception e) {
            log.error("导出项目Markdown失败 projectId={}", projectId, e);
            return ResponseEntity.internalServerError()
                    .body("导出失败: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }

    @GetMapping("/project/{projectId}/latex")
    @Transactional(readOnly = true)
    @Operation(summary = "导出项目为LaTeX", description = "将项目下所有文档导出为单个LaTeX文件")
    public ResponseEntity<String> exportProjectToLatex(@PathVariable Long projectId) {
        try {
            String content = latexExporter.exportProject(projectId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/x-tex; charset=utf-8"));
            headers.setContentDispositionFormData("attachment",
                    "project_" + projectId + ".tex");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(content);
        } catch (Exception e) {
            log.error("导出项目LaTeX失败 projectId={}", projectId, e);
            return ResponseEntity.internalServerError()
                    .body("导出失败: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }
}
