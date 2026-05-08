import React, { useState, useEffect, useCallback } from 'react';
import { Table, Button, Modal, Form, Input, Select, Space, Popconfirm, message, Tag } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, CheckCircleOutlined } from '@ant-design/icons';
import { documentApi } from '../services/api';
import { useAppStore } from '../stores/appStore';

const DocumentList: React.FC = () => {
  const { currentProject, currentDocument, setCurrentDocument, documents, setDocuments, setEndpoints } = useAppStore();
  const [loading, setLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingDocument, setEditingDocument] = useState<any>(null);
  const [form] = Form.useForm();
  const [selectingDocId, setSelectingDocId] = useState<number | null>(null);

  useEffect(() => {
    if (currentProject?.id) {
      fetchDocuments();
    }
  }, [currentProject]);

  const fetchDocuments = async () => {
    if (!currentProject?.id) return;
    setLoading(true);
    try {
      const res = await documentApi.list(currentProject.id);
      setDocuments(res.data);
    } catch (error) {
      message.error('Failed to load documents');
    } finally {
      setLoading(false);
    }
  };

  const handleSelectDocument = useCallback((doc: any) => {
    // Prevent double-click issues
    if (selectingDocId === doc.id) return;

    setSelectingDocId(doc.id);

    // Use setTimeout to ensure state updates are properly sequenced
    setTimeout(() => {
      if (currentDocument?.id === doc.id) {
        // Deselect - clear both state at once in a stable order
        setEndpoints([]);
        setCurrentDocument(null);
      } else {
        // Select new document
        setEndpoints([]);
        setCurrentDocument(doc);
      }
      setSelectingDocId(null);
    }, 0);
  }, [currentDocument, selectingDocId, setCurrentDocument, setEndpoints]);

  const handleCreate = () => {
    setEditingDocument(null);
    form.resetFields();
    setModalVisible(true);
  };

  const handleEdit = (record: any) => {
    setEditingDocument(record);
    form.setFieldsValue({
      name: record.name,
      description: record.description,
      version: record.version,
      status: record.status,
      tags: record.tags
    });
    setModalVisible(true);
  };

  const handleDelete = async (id: number) => {
    try {
      await documentApi.delete(id);
      message.success('Document deleted');

      // Check if we need to clear current document after a delay
      if (currentDocument?.id === id) {
        setTimeout(() => {
          setEndpoints([]);
          setCurrentDocument(null);
        }, 0);
      }

      // Refresh document list
      setTimeout(() => {
        fetchDocuments();
      }, 50);
    } catch (error) {
      message.error('Failed to delete document');
    }
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      // 构建发送给后端的数据
      const data = {
        projectId: currentProject?.id,
        name: values.name,
        description: values.description || '',
        version: values.version || '1.0.0',
        status: values.status || 'DRAFT',
        tags: values.tags || '',
        sortOrder: 0
      };

      if (editingDocument) {
        await documentApi.update(editingDocument.id, data);
        message.success('Document updated');
      } else {
        await documentApi.create(data);
        message.success('Document created');
      }
      setModalVisible(false);
      fetchDocuments();
    } catch (error) {
      message.error('Operation failed');
    }
  };

  const columns = [
    {
      title: 'Selected',
      key: 'selected',
      width: 80,
      render: (_: any, record: any) => (
        currentDocument?.id === record.id ? (
          <Tag color="blue" icon={<CheckCircleOutlined />}>Active</Tag>
        ) : null
      ),
    },
    { title: 'Name', dataIndex: 'name', key: 'name' },
    { title: 'Description', dataIndex: 'description', key: 'description', render: (v: string) => v || '-' },
    { title: 'Version', dataIndex: 'version', key: 'version', render: (v: string) => v || '-' },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => (
        <Tag color={status === 'PUBLISHED' ? 'green' : status === 'DEPRECATED' ? 'red' : 'orange'}>
          {status || 'DRAFT'}
        </Tag>
      ),
    },
    {
      title: 'Actions',
      key: 'actions',
      width: 200,
      render: (_: any, record: any) => (
        <Space size="small">
          <Button
            type="primary"
            size="small"
            onClick={() => handleSelectDocument(record)}
          >
            {currentDocument?.id === record.id ? 'Deselect' : 'Select'}
          </Button>
          <Button
            type="default"
            size="small"
            icon={<EditOutlined />}
            onClick={() => handleEdit(record)}
          >
            Edit
          </Button>
          <Popconfirm
            title="Delete this document?"
            description="This will permanently delete the document and all its endpoints"
            onConfirm={() => handleDelete(record.id)}
            okText="Delete"
            cancelText="Cancel"
            okButtonProps={{ danger: true }}
          >
            <Button
              type="primary"
              size="small"
              danger
              icon={<DeleteOutlined />}
            >
              Delete
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <span>Project: <strong>{currentProject?.name}</strong></span>
        </div>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
          New Document
        </Button>
      </div>
      <Table
        dataSource={documents}
        columns={columns}
        rowKey="id"
        loading={loading}
        rowClassName={(record) => currentDocument?.id === record.id ? 'ant-table-row-selected' : ''}
      />
      <Modal
        title={editingDocument ? 'Edit Document' : 'New Document'}
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={() => setModalVisible(false)}
        width={600}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="Document Name" rules={[{ required: true, message: 'Please enter document name' }]}>
            <Input placeholder="Enter document name" />
          </Form.Item>
          <Form.Item name="description" label="Description">
            <Input.TextArea rows={3} placeholder="Enter description" />
          </Form.Item>
          <Form.Item name="version" label="Version">
            <Input placeholder="1.0.0" />
          </Form.Item>
          <Form.Item name="status" label="Status">
            <Select
              options={[
                { label: 'Draft', value: 'DRAFT' },
                { label: 'Published', value: 'PUBLISHED' },
                { label: 'Deprecated', value: 'DEPRECATED' },
              ]}
              placeholder="Select status"
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default DocumentList;
