import React, { useState, useEffect } from 'react';
import { Card, Button, Space, message, Popconfirm, Empty, Collapse, Tag, Divider } from 'antd';
import { FolderOutlined, FileOutlined, DeleteOutlined, ReloadOutlined, PlusOutlined } from '@ant-design/icons';
import { parameterTemplateApi } from '../services/api';
import { useAppStore } from '../stores/appStore';

interface ParameterTemplate {
  id: number;
  folderName: string;
  templateName: string;
  parameters: any[];
  documentId: number;
}

const ParameterTemplatePanel: React.FC = () => {
  const { currentDocument, setCurrentPage } = useAppStore();
  const [loading, setLoading] = useState(false);
  const [folders, setFolders] = useState<string[]>([]);
  const [templates, setTemplates] = useState<Record<string, ParameterTemplate[]>>({});

  useEffect(() => {
    if (currentDocument?.id) {
      loadTemplates();
    }
  }, [currentDocument]);

  const loadTemplates = async () => {
    if (!currentDocument?.id) return;
    
    setLoading(true);
    try {
      const res = await parameterTemplateApi.getFolders(currentDocument.id);
      setFolders(res.data || []);
      
      const templatesMap: Record<string, ParameterTemplate[]> = {};
      for (const folder of res.data || []) {
        if (folder && folder.trim()) {
          const folderRes = await parameterTemplateApi.getByFolder(folder, currentDocument.id);
          templatesMap[folder] = folderRes.data || [];
        }
      }
      setTemplates(templatesMap);
    } catch (error) {
      message.error('加载模板失败');
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteTemplate = async (id: number) => {
    try {
      await parameterTemplateApi.delete(id);
      message.success('删除成功');
      loadTemplates();
    } catch (error) {
      message.error('删除失败');
    }
  };

  const handleDeleteFolder = async (folderName: string) => {
    try {
      await parameterTemplateApi.deleteFolder(folderName, currentDocument!.id);
      message.success('文件夹删除成功');
      loadTemplates();
    } catch (error) {
      message.error('删除失败');
    }
  };

  const renderParameterList = (params: any[]) => {
    if (!params || params.length === 0) {
      return <div style={{ color: '#999' }}>无参数</div>;
    }

    return (
      <div style={{ marginTop: 8 }}>
        {params.map((param: any, index: number) => (
          <div
            key={index}
            style={{
              padding: '4px 8px',
              background: '#f5f5f5',
              borderRadius: 4,
              marginBottom: 4,
              fontSize: 12,
            }}
          >
            <strong>{param.name}</strong>
            <span style={{ color: '#666', marginLeft: 8 }}>
              [{param.dataType || 'string'}]
            </span>
            {param.required && <Tag color="red" style={{ marginLeft: 4 }}>必填</Tag>}
          </div>
        ))}
      </div>
    );
  };

  const collapseItems = folders.map((folder) => ({
    key: folder,
    label: (
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Space>
          <FolderOutlined />
          <span style={{ fontWeight: 500 }}>{folder}</span>
          <Tag color="blue">{templates[folder]?.length || 0} 个模板</Tag>
        </Space>
        <Popconfirm
          title="确定删除此文件夹及所有模板？"
          onConfirm={() => handleDeleteFolder(folder)}
          okText="确定"
          cancelText="取消"
        >
          <Button
            type="text"
            danger
            icon={<DeleteOutlined />}
            size="small"
            onClick={(e) => e.stopPropagation()}
          />
        </Popconfirm>
      </div>
    ),
    children: (
      <div>
        {templates[folder] && templates[folder].length > 0 ? (
          templates[folder].map((template) => (
            <Card
              key={template.id}
              size="small"
              style={{ marginBottom: 8 }}
              title={
                <Space>
                  <FileOutlined />
                  <span>{template.templateName}</span>
                </Space>
              }
              extra={
                <Popconfirm
                  title="确定删除此模板？"
                  onConfirm={() => handleDeleteTemplate(template.id)}
                  okText="确定"
                  cancelText="取消"
                >
                  <Button type="text" danger icon={<DeleteOutlined />} size="small" />
                </Popconfirm>
              }
            >
              <div>
                <strong style={{ fontSize: 12, color: '#666' }}>参数列表：</strong>
                {renderParameterList(template.parameters)}
              </div>
            </Card>
          ))
        ) : (
          <Empty description="暂无模板" image={Empty.PRESENTED_IMAGE_SIMPLE} />
        )}
      </div>
    ),
  }));

  return (
    <div style={{ padding: 16 }}>
      <Card
        title={
          <Space>
            <FolderOutlined />
            <span>参数模板管理</span>
          </Space>
        }
        extra={
          <Space>
            <Button
              icon={<ReloadOutlined />}
              onClick={loadTemplates}
              loading={loading}
            >
              刷新
            </Button>
          </Space>
        }
      >
        {!currentDocument ? (
          <Empty description="请先选择一个文档" />
        ) : folders.length === 0 ? (
          <Empty
            description={
              <div>
                <p style={{ color: '#999' }}>暂无参数模板</p>
                <p style={{ color: '#999', fontSize: 12 }}>
                  在创建接口时，点击"保存为模板"按钮来创建参数模板
                </p>
              </div>
            }
            image={Empty.PRESENTED_IMAGE_SIMPLE}
          />
        ) : (
          <Collapse items={collapseItems} defaultActiveKey={[]} />
        )}
      </Card>
    </div>
  );
};

export default ParameterTemplatePanel;
