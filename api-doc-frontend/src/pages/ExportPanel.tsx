import React, { useState } from 'react';
import { Card, Button, Space, Segmented, Typography, message, Tabs, Tag, Divider, Steps, Alert } from 'antd';
import { DownloadOutlined, FileMarkdownOutlined, FilePdfOutlined, EyeOutlined, CopyOutlined } from '@ant-design/icons';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import rehypeKatex from 'rehype-katex';
import remarkMath from 'remark-math';
import 'katex/dist/katex.min.css';
import { exportApi } from '../services/api';
import { useAppStore } from '../stores/appStore';

const { Title, Text, Paragraph } = Typography;

/**
 * 文档导出面板
 * 支持导出为Markdown（含LaTeX公式）和LaTeX格式
 */
const ExportPanel: React.FC = () => {
  const { currentDocument, currentProject } = useAppStore();
  const [exportFormat, setExportFormat] = useState<'markdown' | 'latex'>('markdown');
  const [previewContent, setPreviewContent] = useState<string>('');
  const [loading, setLoading] = useState(false);
  const [previewMode, setPreviewMode] = useState(false);

  /**
   * 处理文档导出
   */
  const handleExport = async () => {
    if (!currentDocument?.id) {
      message.warning('请先选择一个文档');
      return;
    }
    setLoading(true);
    try {
      let content: string;
      let filename: string;
      let mimeType: string;

      if (exportFormat === 'markdown') {
        const res = await exportApi.exportMarkdown(currentDocument.id);
        content = res.data;
        filename = `${currentDocument.name}.md`;
        mimeType = 'text/markdown;charset=utf-8';
      } else {
        const res = await exportApi.exportLatex(currentDocument.id);
        content = res.data;
        filename = `${currentDocument.name}.tex`;
        mimeType = 'text/x-tex;charset=utf-8';
      }

      // 创建下载
      const blob = new Blob([content], { type: mimeType });
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = filename;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(url);

      message.success('导出成功');
    } catch (error) {
      message.error('导出失败');
    } finally {
      setLoading(false);
    }
  };

  /**
   * 处理预览
   */
  const handlePreview = async () => {
    if (!currentDocument?.id) {
      message.warning('请先选择一个文档');
      return;
    }
    setLoading(true);
    try {
      let content: string;
      if (exportFormat === 'markdown') {
        const res = await exportApi.exportMarkdown(currentDocument.id);
        content = res.data;
      } else {
        const res = await exportApi.exportLatex(currentDocument.id);
        content = res.data;
      }
      setPreviewContent(content);
      setPreviewMode(true);
    } catch (error) {
      message.error('预览失败');
    } finally {
      setLoading(false);
    }
  };

  /**
   * 复制内容到剪贴板
   */
  const handleCopy = () => {
    navigator.clipboard.writeText(previewContent);
    message.success('已复制到剪贴板');
  };

  return (
    <div>
      {/* 导出控制面板 */}
      <Card
        title="文档导出"
        style={{ marginBottom: 16 }}
        extra={
          <Tag color="blue">
            {currentProject?.name || '未选择项目'} / {currentDocument?.name || '未选择文档'}
          </Tag>
        }
      >
        <Space direction="vertical" style={{ width: '100%' }} size="middle">
          {/* 格式选择 */}
          <div>
            <Text strong>导出格式：</Text>
            <Segmented
              value={exportFormat}
              onChange={(value) => setExportFormat(value as 'markdown' | 'latex')}
              options={[
                {
                  label: (
                    <Space>
                      <FileMarkdownOutlined />
                      <span>Markdown</span>
                    </Space>
                  ),
                  value: 'markdown'
                },
                {
                  label: (
                    <Space>
                      <FilePdfOutlined />
                      <span>LaTeX</span>
                    </Space>
                  ),
                  value: 'latex'
                },
              ]}
              style={{ marginTop: 8 }}
            />
          </div>

          <Divider style={{ margin: '8px 0' }} />

          {/* 操作按钮 */}
          <Space>
            <Button
              icon={<EyeOutlined />}
              onClick={handlePreview}
              loading={loading}
            >
              预览
            </Button>
            <Button
              type="primary"
              icon={<DownloadOutlined />}
              onClick={handleExport}
              loading={loading}
            >
              下载{exportFormat === 'markdown' ? 'Markdown' : 'LaTeX'}文件
            </Button>
          </Space>
        </Space>
      </Card>

      {/* 预览区域 */}
      {previewMode && (
        <Card
          title="文档预览"
          style={{ marginBottom: 16 }}
          extra={
            <Space>
              <Button
                type="text"
                icon={<CopyOutlined />}
                onClick={handleCopy}
              >
                复制内容
              </Button>
              <Button type="link" onClick={() => setPreviewMode(false)}>
                关闭预览
              </Button>
            </Space>
          }
        >
          <div style={{
            background: '#fafafa',
            padding: 24,
            borderRadius: 8,
            maxHeight: 'calc(100vh - 350px)',
            overflow: 'auto',
            border: '1px solid #e8e8e8'
          }}>
            {exportFormat === 'markdown' ? (
              <div className="markdown-preview">
                <ReactMarkdown
                  remarkPlugins={[remarkGfm, remarkMath]}
                  rehypePlugins={[rehypeKatex]}
                >
                  {previewContent}
                </ReactMarkdown>
              </div>
            ) : (
              <div>
                <Alert type="info" message="LaTeX格式预览" description="以下为LaTeX源代码，可使用LaTeX编辑器（如TeX Live、Overleaf）编译为PDF。" showIcon style={{ marginBottom: 16 }} />
                <pre style={{
                  whiteSpace: 'pre-wrap',
                  fontFamily: 'Consolas, Monaco, monospace',
                  background: '#f5f5f5',
                  padding: 16,
                  borderRadius: 4,
                  overflow: 'auto'
                }}>
                  {previewContent}
                </pre>
              </div>
            )}
          </div>
        </Card>
      )}

      {/* 格式说明 */}
      <Card title="导出格式说明">
        <Tabs
          items={[
            {
              key: 'markdown',
              label: 'Markdown格式',
              children: (
                <div>
                  <Title level={5}>功能特点：</Title>
                  <ul>
                    <li>支持GitHub Flavored Markdown (GFM)标准</li>
                    <li>支持LaTeX数学公式（行内公式和块级公式）</li>
                    <li>自动生成表格和目录结构</li>
                    <li>代码块语法高亮</li>
                    <li>易于版本控制管理</li>
                    <li>可直接在GitHub、GitLab等平台展示</li>
                  </ul>

                  <Title level={5}>LaTeX公式示例：</Title>
                  <Card size="small" style={{ background: '#f9f9f9' }}>
                    <Text strong>行内公式：</Text>
                    <code style={{ display: 'block', marginTop: 8 }}>{'$QPS = \\frac{Total}{Time}$'}</code>
                    <Text strong style={{ marginTop: 16, display: 'block' }}>块级公式：</Text>
                    <pre style={{ background: '#fff', padding: 8, borderRadius: 4 }}>
{`$$
\\text{系统吞吐量} = \\frac{\\text{成功请求数}}{\\text{总时间}}
$$`}
                    </pre>
                  </Card>
                </div>
              ),
            },
            {
              key: 'latex',
              label: 'LaTeX格式',
              children: (
                <div>
                  <Title level={5}>功能特点：</Title>
                  <ul>
                    <li>专业排版效果</li>
                    <li>完整的数学公式支持（amsmath）</li>
                    <li>中文排版支持（xeCJK）</li>
                    <li>代码高亮支持（minted）</li>
                    <li>书签和超链接支持</li>
                    <li>可直接编译为PDF文档</li>
                  </ul>

                  <Title level={5}>编译要求：</Title>
                  <Card size="small" style={{ background: '#f9f9f9' }}>
                    <Text>需要安装以下LaTeX包：</Text>
                    <ul>
                      <li>xeCJK - 中文支持</li>
                      <li>amsmath, amssymb, amsfonts - 数学公式</li>
                      <li>listings - 代码高亮</li>
                      <li>booktabs, longtable - 表格</li>
                      <li>hyperref - 超链接</li>
                    </ul>
                    <Text type="secondary">
                      推荐使用TeX Live或Overleaf在线编辑器
                    </Text>
                  </Card>
                </div>
              ),
            },
            {
              key: 'postman',
              label: 'Postman Collection',
              children: (
                <div>
                  <Title level={5}>功能特点：</Title>
                  <ul>
                    <li>一键导入Postman</li>
                    <li>自动生成请求示例</li>
                    <li>支持环境变量配置</li>
                    <li>测试脚本自动生成</li>
                  </ul>

                  <Title level={5}>使用方法：</Title>
                  <Steps
                    items={[
                      { title: '导出文件', description: '下载JSON文件' },
                      { title: '打开Postman', description: '启动Postman应用' },
                      { title: '导入集合', description: '点击Import按钮' },
                      { title: '配置环境', description: '设置Base URL等变量' },
                    ]}
                  />
                </div>
              ),
            },
          ]}
        />
      </Card>

      <style>{`
        .markdown-preview {
          font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
          line-height: 1.6;
          color: #24292e;
        }
        .markdown-preview h1,
        .markdown-preview h2,
        .markdown-preview h3,
        .markdown-preview h4 {
          margin-top: 24px;
          margin-bottom: 16px;
          font-weight: 600;
        }
        .markdown-preview h1 {
          font-size: 2em;
          border-bottom: 1px solid #eaecef;
          padding-bottom: 0.3em;
        }
        .markdown-preview h2 {
          font-size: 1.5em;
          border-bottom: 1px solid #eaecef;
          padding-bottom: 0.3em;
        }
        .markdown-preview code {
          padding: 0.2em 0.4em;
          margin: 0;
          font-size: 85%;
          background-color: rgba(27,31,35,0.05);
          border-radius: 3px;
          font-family: Consolas, "Liberation Mono", Menlo, monospace;
        }
        .markdown-preview pre {
          padding: 16px;
          overflow: auto;
          font-size: 85%;
          line-height: 1.45;
          background-color: #f6f8fa;
          border-radius: 3px;
        }
        .markdown-preview pre code {
          padding: 0;
          margin: 0;
          background-color: transparent;
          border: 0;
        }
        .markdown-preview table {
          border-collapse: collapse;
          width: 100%;
          margin: 16px 0;
        }
        .markdown-preview table th,
        .markdown-preview table td {
          padding: 6px 13px;
          border: 1px solid #dfe2e5;
        }
        .markdown-preview table tr:nth-child(2n) {
          background-color: #f6f8fa;
        }
        .markdown-preview blockquote {
          padding: 0 1em;
          color: #6a737d;
          border-left: 0.25em solid #dfe2e5;
          margin: 0;
        }
        .markdown-preview ul,
        .markdown-preview ol {
          padding-left: 2em;
          margin: 16px 0;
        }
      `}</style>
    </div>
  );
};

export default ExportPanel;
