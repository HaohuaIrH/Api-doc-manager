import React, { useState, useEffect } from 'react';
import { Modal, Collapse, Button, Space, message, Popconfirm, Empty, Tag } from 'antd';
import { FolderOutlined, FileOutlined, DeleteOutlined, ReloadOutlined, RightOutlined } from '@ant-design/icons';
import { parameterTemplateApi } from '../services/api';

interface ParameterTemplate {
  id: number;
  folderName: string;
  templateName: string;
  parameters: any[];
  documentId: number;
}

interface ParameterTemplateSelectorProps {
  visible: boolean;
  documentId: number;
  onClose: () => void;
  onSelect: (template: ParameterTemplate) => void;
}

const ParameterTemplateSelector: React.FC<ParameterTemplateSelectorProps> = ({
  visible,
  documentId,
  onClose,
  onSelect,
}) => {
  const [loading, setLoading] = useState(false);
  const [folders, setFolders] = useState<string[]>([]);
  const [templates, setTemplates] = useState<Record<string, ParameterTemplate[]>>({});
  const [activeKeys, setActiveKeys] = useState<string[]>([]);

  useEffect(() => {
    if (visible && documentId) {
      loadFolders();
    }
  }, [visible, documentId]);

  const loadFolders = async () => {
    if (!documentId) return;

    setLoading(true);
    try {
      const res = await parameterTemplateApi.getFolders(documentId);
      setFolders(res.data || []);

      const templatesMap: Record<string, ParameterTemplate[]> = {};
      for (const folder of res.data || []) {
        if (folder && folder.trim()) {
          const folderRes = await parameterTemplateApi.getByFolder(folder, documentId);
          templatesMap[folder] = folderRes.data || [];
        }
      }
      setTemplates(templatesMap);
    } catch (error: any) {
      console.error('加载模板失败:', error);
      const errorMsg = error.response?.data?.message || error.message || '加载模板失败';
      message.error(errorMsg);
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteTemplate = async (id: number) => {
    try {
      await parameterTemplateApi.delete(id);
      message.success('删除成功');
      loadFolders();
    } catch (error) {
      message.error('删除失败');
    }
  };

  const handleDeleteFolder = async (folderName: string) => {
    try {
      await parameterTemplateApi.deleteFolder(folderName, documentId);
      message.success('文件夹删除成功');
      setActiveKeys(activeKeys.filter(k => k !== folderName));
      loadFolders();
    } catch (error) {
      message.error('删除失败');
    }
  };

  const renderParameterPreview = (params: any[]) => {
    if (!params || params.length === 0) return null;
    
    const preview = params.slice(0, 3).map(p => p.name).join(', ');
    const more = params.length > 3 ? ` 等${params.length}个参数` : '';
    return (
      <span style={{ color: '#999', fontSize: 12 }}>
        {preview}{more}
      </span>
    );
  };

  const collapseItems = folders.map((folder) => ({
    key: folder,
    label: (
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', width: '100%' }}>
        <Space>
          <FolderOutlined />
          <span style={{ fontWeight: 500 }}>{folder}</span>
          <Tag color="blue">{templates[folder]?.length || 0}</Tag>
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
            <div
              key={template.id}
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                padding: '12px',
                marginBottom: 8,
                background: '#f5f5f5',
                borderRadius: 6,
                border: '1px solid #e8e8e8',
              }}
            >
              <div style={{ flex: 1 }}>
                <Space>
                  <FileOutlined />
                  <span style={{ fontWeight: 500 }}>{template.templateName}</span>
                </Space>
                <div style={{ marginTop: 4 }}>
                  {renderParameterPreview(template.parameters)}
                </div>
              </div>
              <Space>
                <Button
                  type="primary"
                  size="small"
                  onClick={() => onSelect(template)}
                >
                  使用
                </Button>
                <Popconfirm
                  title="确定删除此模板？"
                  onConfirm={() => handleDeleteTemplate(template.id)}
                  okText="确定"
                  cancelText="取消"
                >
                  <Button
                    type="text"
                    danger
                    icon={<DeleteOutlined />}
                    size="small"
                  />
                </Popconfirm>
              </Space>
            </div>
          ))
        ) : (
          <Empty description="暂无模板" image={Empty.PRESENTED_IMAGE_SIMPLE} />
        )}
      </div>
    ),
  }));

  return (
    <Modal
      title={
        <Space>
          <FolderOutlined />
          <span>选择参数模板</span>
        </Space>
      }
      open={visible}
      onCancel={onClose}
      footer={null}
      width={650}
    >
      <div style={{ marginBottom: 16 }}>
        <Button
          icon={<ReloadOutlined />}
          onClick={loadFolders}
          loading={loading}
        >
          刷新
        </Button>
      </div>

      {loading ? (
        <div style={{ textAlign: 'center', padding: 40 }}>
          加载中...
        </div>
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
        <Collapse
          items={collapseItems}
          activeKey={activeKeys}
          onChange={(keys) => setActiveKeys(keys as string[])}
          expandIcon={({ isActive }) => 
            <RightOutlined rotate={isActive ? 90 : 0} />
          }
        />
      )}
    </Modal>
  );
};

export default ParameterTemplateSelector;
