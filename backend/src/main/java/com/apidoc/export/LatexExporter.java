package com.apidoc.export;

import com.apidoc.entity.*;
import com.apidoc.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LatexExporter {

    private final ApiDocumentRepository documentRepository;
    private final ApiEndpointRepository endpointRepository;
    private final ApiParameterRepository parameterRepository;
    private final ApiResponseRepository responseRepository;
    private final ProjectRepository projectRepository;

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public String exportProject(Long projectId) {
        try {
            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new RuntimeException("项目不存在: " + projectId));

            StringBuilder sb = new StringBuilder();
            sb.append(generateLatexHeader(project));

            List<ApiDocument> documents = documentRepository.findByProjectId(projectId);

            int docIndex = 1;
            for (ApiDocument doc : documents) {
                sb.append(exportDocumentToLatex(doc, docIndex));
                docIndex++;
            }

            sb.append(generateLatexAppendix());
            sb.append("\\end{document}\n");

            return sb.toString();
        } catch (Exception e) {
            log.error("导出LaTeX文档失败", e);
            throw new RuntimeException("导出失败: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true, rollbackFor = Exception.class)
    public String exportDocumentToLatex(ApiDocument document, int index) {
        StringBuilder sb = new StringBuilder();

        if (document == null) {
            log.warn("文档为空，跳过导出");
            return sb.toString();
        }

        String name = document.getName();
        // 使用 \section 生成一级章节
        sb.append("\\section{").append(escapeLatex(name != null ? name : "未命名文档")).append("}\n\n");

        String description = document.getDescription();
        if (description != null && !description.isEmpty()) {
            sb.append(escapeLatex(description)).append("\n\n");
        }

        // 直接查询接口，避免懒加载
        List<ApiEndpoint> endpoints = endpointRepository.findByDocumentId(document.getId());

        int endpointCount = 1;
        for (ApiEndpoint endpoint : endpoints) {
            if (endpoint != null) {
                sb.append(exportEndpointToLatex(endpoint, index, endpointCount));
                endpointCount++;
            }
        }

        return sb.toString();
    }

    public String exportEndpointToLatex(ApiEndpoint endpoint, int docIndex, int endpointIndex) {
        StringBuilder sb = new StringBuilder();

        // 生成子章节标题：序号. 方法 路径
        String method = endpoint.getMethod() != null ? endpoint.getMethod().name() : "GET";
        String path = endpoint.getPath() != null ? endpoint.getPath() : "/";
        
        sb.append("\\subsection{").append(endpointIndex).append(". ");
        sb.append("\\textbf{").append(method).append("} ");
        sb.append("\\texttt{").append(escapeLatex(path)).append("}");
        sb.append("}\\nopagebreak\n\n");

        String summary = endpoint.getSummary();
        if (summary != null && !summary.isEmpty()) {
            sb.append("\\textit{").append(escapeLatex(summary)).append("}\n\n");
        }

        String description = endpoint.getDescription();
        if (description != null && !description.isEmpty()) {
            sb.append(escapeLatex(description)).append("\n\n");
        }

        // 直接查询参数，避免懒加载
        List<ApiParameter> params = parameterRepository.findByEndpointId(endpoint.getId());
        
        if (!params.isEmpty()) {
            sb.append("\\textbf{参数说明}\n\n");
            
            // 按位置分组
            Map<ApiParameter.ParameterLocation, List<ApiParameter>> groupedParams = params.stream()
                    .collect(Collectors.groupingBy(ApiParameter::getLocation));

            // 1. 路径参数 (Path)
            if (groupedParams.containsKey(ApiParameter.ParameterLocation.PATH)) {
                sb.append(generateParamTableLatex(groupedParams.get(ApiParameter.ParameterLocation.PATH), "路径参数"));
            }

            // 2. 查询参数 (Query)
            if (groupedParams.containsKey(ApiParameter.ParameterLocation.QUERY)) {
                sb.append(generateParamTableLatex(groupedParams.get(ApiParameter.ParameterLocation.QUERY), "查询参数"));
            }

            // 3. 请求头参数 (Header)
            if (groupedParams.containsKey(ApiParameter.ParameterLocation.HEADER)) {
                sb.append(generateParamTableLatex(groupedParams.get(ApiParameter.ParameterLocation.HEADER), "请求头参数"));
            }

            // 4. 请求体参数 (Body)
            if (groupedParams.containsKey(ApiParameter.ParameterLocation.REQUEST_BODY)) {
                sb.append(generateParamTableLatex(groupedParams.get(ApiParameter.ParameterLocation.REQUEST_BODY), "请求体参数"));
            }
        }

        // 直接查询响应，避免懒加载
        List<ApiResponse> responses = responseRepository.findByEndpointId(endpoint.getId());
        
        if (!responses.isEmpty()) {
            sb.append("\\textbf{响应说明}\n\n");
            sb.append("\\begin{itemize}\n");
            for (ApiResponse response : responses) {
                if (response.getStatusCode() != null) {
                    sb.append("\\item \\textbf{").append(response.getStatusCode()).append("}");
                    if (response.getDescription() != null && !response.getDescription().isEmpty()) {
                        sb.append(": ").append(escapeLatex(response.getDescription()));
                    }
                    sb.append("\n");
                }
            }
            sb.append("\\end{itemize}\n\n");
        }

        sb.append("\\vspace{0.5cm}\n\\hrule\n\\vspace{0.5cm}\n\n"); // 添加分隔线
        return sb.toString();
    }

    /**
     * 生成参数表格 LaTeX 代码
     */
    private String generateParamTableLatex(List<ApiParameter> params, String title) {
        if (params == null || params.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\\subsubsection*{").append(title).append("}\n");
        sb.append("\\begin{table}[h]\n");
        sb.append("\\centering\n");
        // 列定义：左对齐，左对齐，居中，左侧固定宽度自动换行
        sb.append("\\begin{tabular}{llcp{6cm}}\n"); 
        sb.append("\\toprule\n");
        sb.append("\\textbf{参数名} & \\textbf{类型} & \\textbf{必填} & \\textbf{描述} \\\\\n");
        sb.append("\\midrule\n");

        for (ApiParameter param : params) {
            String name = escapeLatex(param.getName() != null ? param.getName() : "-");
            String type = escapeLatex(param.getDataType() != null ? param.getDataType() : "string");
            String required = Boolean.TRUE.equals(param.getRequired()) ? "是" : "否";
            String desc = escapeLatex(param.getDescription() != null ? param.getDescription() : "-");
            
            // 注意：在 p{} 列中，& 不需要额外转义，但内容本身需要 escapeLatex
            sb.append(name).append(" & ")
              .append(type).append(" & ")
              .append(required).append(" & ")
              .append(desc).append(" \\\\\n");
        }

        sb.append("\\bottomrule\n");
        sb.append("\\end{tabular}\n");
        sb.append("\\end{table}\n\n");
        
        return sb.toString();
    }

    private String generateLatexHeader(Project project) {
        StringBuilder sb = new StringBuilder();

        sb.append("% =====================================================\n");
        sb.append("% API文档自动生成\n");
        sb.append("% 项目: ").append(escapeLatex(project.getName())).append("\n");
        sb.append("% 版本: ").append(escapeLatex(project.getVersion())).append("\n");
        sb.append("% 生成时间: \\today\n");
        sb.append("% 注意：请使用 XeLaTeX 编译器编译此文件以支持中文\n");
        sb.append("% =====================================================\n\n");

        sb.append("\\documentclass[12pt,a4paper]{article}\n\n");

        sb.append("% 中文支持 (必须使用 XeLaTeX 编译)\n");
        sb.append("\\usepackage{ctex}\n\n");

        sb.append("% 页面布局\n");
        sb.append("\\usepackage{geometry}\n");
        sb.append("\\geometry{left=2.5cm,right=2.5cm,top=3cm,bottom=2.5cm}\n\n");

        sb.append("% 代码高亮支持\n");
        sb.append("\\usepackage{listings}\n");
        sb.append("\\usepackage{xcolor}\n");
        sb.append("\\lstset{\n");
        sb.append("    breaklines=true,\n");
        sb.append("    frame=single,\n");
        sb.append("    basicstyle=\\ttfamily\\small,\n");
        sb.append("    backgroundcolor=\\color{gray!10}\n");
        sb.append("}\n\n");

        sb.append("% 表格美化支持\n");
        sb.append("\\usepackage{booktabs}\n");
        sb.append("\\usepackage{array}\n\n"); // array 包用于增强表格列定义

        sb.append("\\begin{document}\n\n");

        sb.append("\\title{").append(escapeLatex(project.getName() != null ? project.getName() : "API文档")).append("}\n");
        sb.append("\\author{API Document Manager}\n");
        sb.append("\\date{\\today}\n\n");

        sb.append("\\maketitle\n\n");

        if (project.getDescription() != null && !project.getDescription().isEmpty()) {
            sb.append("\\begin{abstract}\n");
            sb.append(escapeLatex(project.getDescription()));
            sb.append("\\end{abstract}\n\n");
        }

        if (project.getBaseUrl() != null && !project.getBaseUrl().isEmpty()) {
            sb.append("\\noindent \\textbf{Base URL:} \\texttt{").append(escapeLatex(project.getBaseUrl())).append("}\n\n");
        }

        sb.append("\\tableofcontents\n");
        sb.append("\\newpage\n\n");

        return sb.toString();
    }

    private String generateLatexAppendix() {
        StringBuilder sb = new StringBuilder();
        sb.append("\\newpage\n");
        sb.append("\\section*{附录}\n");
        sb.append("\\addcontentsline{toc}{section}{附录}\n\n");
        sb.append("本文档由 API Document Manager 自动生成。\n");
        return sb.toString();
    }

    /**
     * 转义 LaTeX 特殊字符
     * 注意：必须先处理反斜杠 \
     */
    private String escapeLatex(String text) {
        if (text == null) {
            return "";
        }
        
        // 1. 处理反斜杠 (必须第一步)
        String result = text.replace("\\", "\\textbackslash{}");
        
        // 2. 处理其他特殊字符
        result = result.replace("{", "\\{")
                       .replace("}", "\\}")
                       .replace("$", "\\$")
                       .replace("&", "\\&")
                       .replace("#", "\\#")
                       .replace("%", "\\%")
                       .replace("_", "\\_")
                       .replace("^", "\\^{}")
                       .replace("~", "\\~{}")
                       .replace("|", "\\textbar{}")
                       .replace("<", "\\textless{}")
                       .replace(">", "\\textgreater{}");
        
        // 3. 处理换行符：在普通文本中，LaTeX忽略单个换行。
        // 如果希望保留用户输入的换行格式，可以将 \n 替换为 \\ (强制换行)
        // 但在表格单元格中，通常不建议使用 \\，而是依靠 p{width} 自动换行。
        // 这里为了通用性，我们将非表格区域的 \n 视为段落分隔或忽略，
        // 如果在描述中出现 \n，通常希望它换行，所以替换为 \\ 
        // 注意：如果在 \section{} 或 \textbf{} 中使用 \\ 可能会报错，需谨慎。
        // 更安全的做法是移除换行符，依靠 LaTeX 的自然段落流，或者仅在某些环境替换。
        // 鉴于我们在描述中直接使用文本，这里将 \n 替换为空格或保留原样让 LaTeX 处理段落。
        // 为了防止破坏命令，这里简单地将 \n 替换为空格，依靠 LaTeX 的段落机制。
        // 如果需要强制换行，可以使用 \newline，但不能在所有章节标题中使用。
        // 折中方案：替换为 ~ (不可断行空格) 或者简单地移除，让 LaTeX 重新排版。
        // 这里选择移除多余空白，保持整洁。
        result = result.replace("\n", " ").replace("\r", "");
        
        return result;
    }
}