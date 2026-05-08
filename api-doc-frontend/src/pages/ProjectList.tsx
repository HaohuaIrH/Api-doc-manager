import React, { useState, useEffect } from 'react';
import { Table, Button, Modal, Form, Input, Space, Popconfirm, message, Tag } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, CheckCircleOutlined } from '@ant-design/icons';
import { projectApi } from '../services/api';
import { useAppStore } from '../stores/appStore';

const ProjectList: React.FC = () => {
  const { projects, setProjects, currentProject, setCurrentProject, setCurrentDocument, setEndpoints } = useAppStore();
  const [loading, setLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingProject, setEditingProject] = useState<any>(null);
  const [form] = Form.useForm();

  useEffect(() => {
    fetchProjects();
  }, []);

  const fetchProjects = async () => {
    setLoading(true);
    try {
      const res = await projectApi.list();
      setProjects(res.data);
    } catch (error) {
      message.error('Failed to load projects');
    } finally {
      setLoading(false);
    }
  };

  const handleSelectProject = (project: any) => {
    if (currentProject?.id === project.id) {
      setCurrentProject(null);
      setCurrentDocument(null);
      setEndpoints([]);
    } else {
      setCurrentProject(project);
      setCurrentDocument(null);
      setEndpoints([]);
    }
  };

  const handleCreate = () => {
    setEditingProject(null);
    form.resetFields();
    setModalVisible(true);
  };

  const handleEdit = (record: any) => {
    setEditingProject(record);
    form.setFieldsValue(record);
    setModalVisible(true);
  };

  const handleDelete = async (id: number) => {
    try {
      await projectApi.delete(id);
      message.success('Project deleted');
      if (currentProject?.id === id) {
        setCurrentProject(null);
        setCurrentDocument(null);
        setEndpoints([]);
      }
      fetchProjects();
    } catch (error) {
      message.error('Failed to delete project');
    }
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      if (editingProject) {
        await projectApi.update(editingProject.id, values);
        message.success('Project updated');
      } else {
        await projectApi.create(values);
        message.success('Project created');
      }
      setModalVisible(false);
      fetchProjects();
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
        currentProject?.id === record.id ? (
          <Tag color="blue" icon={<CheckCircleOutlined />}>Active</Tag>
        ) : null
      ),
    },
    { title: 'Name', dataIndex: 'name', key: 'name' },
    { title: 'Description', dataIndex: 'description', key: 'description' },
    { title: 'Base URL', dataIndex: 'baseUrl', key: 'baseUrl', render: (url: string) => url || '-' },
    { title: 'Version', dataIndex: 'version', key: 'version', render: (v: string) => v || '-' },
    {
      title: 'Actions',
      key: 'actions',
      width: 200,
      render: (_: any, record: any) => (
        <Space size="small">
          <Button
            type="primary"
            size="small"
            onClick={() => handleSelectProject(record)}
          >
            {currentProject?.id === record.id ? 'Deselect' : 'Select'}
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
            title="Delete this project?"
            description="This will permanently delete the project and all its documents"
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
          {currentProject ? (
            <span>Current Project: <strong>{currentProject.name}</strong></span>
          ) : (
            <span style={{ color: '#888' }}>Select a project to continue</span>
          )}
        </div>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
          New Project
        </Button>
      </div>
      <Table
        dataSource={projects}
        columns={columns}
        rowKey="id"
        loading={loading}
        rowClassName={(record) => currentProject?.id === record.id ? 'ant-table-row-selected' : ''}
      />
      <Modal
        title={editingProject ? 'Edit Project' : 'New Project'}
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={() => setModalVisible(false)}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="Project Name" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="description" label="Description">
            <Input.TextArea />
          </Form.Item>
          <Form.Item name="baseUrl" label="Base URL">
            <Input placeholder="https://api.example.com" />
          </Form.Item>
          <Form.Item name="version" label="Version">
            <Input placeholder="1.0.0" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default ProjectList;
