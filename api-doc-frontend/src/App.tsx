import React, { useEffect, useState } from 'react';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import {
  Button,
  Card,
  Dropdown,
  Empty,
  Form,
  Input,
  Layout,
  List,
  Modal,
  Popconfirm,
  Space,
  Tag,
  message,
} from 'antd';
import {
  ApiOutlined,
  DatabaseOutlined,
  DeleteOutlined,
  EditOutlined,
  ExperimentOutlined,
  ExportOutlined,
  FileTextOutlined,
  FolderOpenOutlined,
  FolderOutlined,
  LogoutOutlined,
  PlusOutlined,
  ProjectOutlined,
  RightOutlined,
  SettingOutlined,
  UserOutlined,
} from '@ant-design/icons';
import { useAppStore } from './stores/appStore';
import { useAuthStore } from './stores/authStore';
import { documentApi, projectApi } from './services/api';
import EndpointEditor from './pages/EndpointEditor';
import ExportPanel from './pages/ExportPanel';
import GlobalParameterPanel from './pages/GlobalParameterPanel';
import LoginPage from './pages/LoginPage';
import ParameterTemplatePanel from './pages/ParameterTemplatePanel';
import TestCaseGenerator from './pages/TestCaseGenerator';
import './App.css';

const { Header, Sider, Content } = Layout;
const { TextArea } = Input;

type ProjectFormValues = {
  name: string;
  description?: string;
  baseUrl?: string;
  version?: string;
};

type DocumentFormValues = {
  name: string;
  description?: string;
  version?: string;
};

const App: React.FC = () => {
  const [projectsOpen, setProjectsOpen] = useState(false);
  const [createDocModalOpen, setCreateDocModalOpen] = useState(false);
  const [createProjectModalOpen, setCreateProjectModalOpen] = useState(false);
  const [editingProject, setEditingProject] = useState<any>(null);
  const [editingDocument, setEditingDocument] = useState<any>(null);
  const [loadingDocuments, setLoadingDocuments] = useState(false);
  const [createDocForm] = Form.useForm<DocumentFormValues>();
  const [createProjectForm] = Form.useForm<ProjectFormValues>();

  const {
    currentDocument,
    currentPage,
    currentProject,
    documents,
    projects,
    setCurrentDocument,
    setCurrentPage,
    setCurrentProject,
    setDocuments,
    setEndpoints,
    setProjects,
  } = useAppStore();
  const { checkAuth, isAuthenticated, logout, user } = useAuthStore();

  useEffect(() => {
    checkAuth();
  }, [checkAuth]);

  useEffect(() => {
    if (isAuthenticated) {
      void loadProjects();
    }
  }, [isAuthenticated]);

  const loadProjects = async () => {
    try {
      const response = await projectApi.list();
      setProjects(response.data);
    } catch (error) {
      console.error('加载项目失败:', error);
      message.error('加载项目失败');
    }
  };

  const loadDocuments = async (projectId: number) => {
    setLoadingDocuments(true);
    try {
      const response = await documentApi.list(projectId);
      setDocuments(response.data);
    } catch (error) {
      console.error('加载文档失败:', error);
      message.error('加载文档失败');
    } finally {
      setLoadingDocuments(false);
    }
  };

  const handleSelectProject = (project: any) => {
    setCurrentProject(project);
    setCurrentDocument(null);
    setEndpoints([]);
    void loadDocuments(project.id);
    setCurrentPage('home');
  };

  const handleSelectDocument = (document: any) => {
    setCurrentDocument(document);
    setCurrentPage('endpoints');
  };

  const openCreateProjectModal = () => {
    setEditingProject(null);
    void createProjectForm.resetFields();
    setCreateProjectModalOpen(true);
  };

  const openCreateDocumentModal = () => {
    setEditingDocument(null);
    void createDocForm.resetFields();
    setCreateDocModalOpen(true);
  };

  const handleEditProject = (project: any, e?: React.MouseEvent) => {
    if (e) {
      e.stopPropagation();
    }
    setEditingProject(project);
    createProjectForm.setFieldsValue({
      name: project.name,
      description: project.description,
      baseUrl: project.baseUrl,
      version: project.version,
    });
    setCreateProjectModalOpen(true);
  };

  const handleEditDocument = (document: any, e?: React.MouseEvent) => {
    if (e) {
      e.stopPropagation();
    }
    setEditingDocument(document);
    createDocForm.setFieldsValue({
      name: document.name,
      description: document.description,
      version: document.version,
    });
    setCreateDocModalOpen(true);
  };

  const handleDeleteProject = async (project: any, e?: React.MouseEvent) => {
    if (e) {
      e.stopPropagation();
    }
    try {
      await projectApi.delete(project.id);
      message.success('项目删除成功');
      if (currentProject?.id === project.id) {
        setCurrentProject(null);
        setCurrentDocument(null);
        setEndpoints([]);
      }
      void loadProjects();
    } catch (error) {
      message.error('删除失败');
    }
  };

  const handleDeleteDocument = async (document: any, e?: React.MouseEvent) => {
    if (e) {
      e.stopPropagation();
    }
    try {
      await documentApi.delete(document.id);
      message.success('文档删除成功');
      if (currentDocument?.id === document.id) {
        setCurrentDocument(null);
        setEndpoints([]);
      }
      if (currentProject) {
        void loadDocuments(currentProject.id);
      }
    } catch (error) {
      message.error('删除失败');
    }
  };

  const handleCreateProject = async (values: ProjectFormValues) => {
    try {
      if (editingProject) {
        await projectApi.update(editingProject.id, values);
        message.success('项目更新成功');
      } else {
        const response = await projectApi.create(values);
        message.success('项目创建成功');
        handleSelectProject(response.data);
      }
      setCreateProjectModalOpen(false);
      setEditingProject(null);
      void createProjectForm.resetFields();
      void loadProjects();
    } catch (error) {
      message.error('操作失败');
    }
  };

  const handleCreateDocument = async (values: DocumentFormValues) => {
    if (!currentProject) {
      message.error('请先选择一个项目');
      return;
    }
    try {
      if (editingDocument) {
        await documentApi.update(editingDocument.id, {
          name: values.name,
          description: values.description,
          projectId: currentProject.id,
          version: values.version,
        });
        message.success('文档更新成功');
      } else {
        await documentApi.create({
          name: values.name,
          description: values.description,
          projectId: currentProject.id,
          version: values.version,
        });
        message.success('文档创建成功');
      }
      setCreateDocModalOpen(false);
      setEditingDocument(null);
      void createDocForm.resetFields();
      void loadDocuments(currentProject.id);
    } catch (error) {
      message.error('操作失败');
    }
  };

  const handleLogout = () => {
    logout();
    setCurrentProject(null);
    setCurrentDocument(null);
    setEndpoints([]);
    setProjects([]);
  };

  const renderEmptyDocumentHint = () => {
    return (
      <div style={{ padding: 24, textAlign: 'center' }}>
        <h4 style={{ margin: 0 }}>请先选择一个文档</h4>
        <p style={{ color: '#999', marginTop: 8 }}>
          请在左侧选择一个项目，然后在该项目下选择一个文档
        </p>
        <Button type="link" onClick={() => setProjectsOpen(true)}>
          打开项目列表
        </Button>
      </div>
    );
  };

  const renderMainContent = () => {
    if (!currentProject) {
      return (
        <div
          style={{
            alignItems: 'center',
            display: 'flex',
            flexDirection: 'column',
            gap: 16,
            height: '100%',
            justifyContent: 'center',
          }}
        >
          <Empty
            description={
              <span style={{ color: '#999', fontSize: 16 }}>
                选择一个项目以查看详情
              </span>
            }
            image={Empty.PRESENTED_IMAGE_SIMPLE}
          />
        </div>
      );
    }

    return (
      <div style={{ padding: 24 }}>
        <Card
          extra={
            <Space>
              <Tag color="blue">v{currentProject.version || '1.0.0'}</Tag>
              <Button
                icon={<EditOutlined />}
                onClick={() => handleEditProject(currentProject)}
              >
                编辑
              </Button>
              <Popconfirm
                title="确定要删除这个项目吗？"
                description="删除后不可恢复，项目下的所有文档都将被删除"
                onConfirm={() => handleDeleteProject(currentProject)}
                okText="删除"
                cancelText="取消"
                okButtonProps={{ danger: true }}
              >
                <Button danger icon={<DeleteOutlined />}>
                  删除
                </Button>
              </Popconfirm>
            </Space>
          }
          style={{ marginBottom: 24 }}
          title={<span><ProjectOutlined /> {currentProject.name}</span>}
        >
          <p>
            <strong>描述：</strong>
            {currentProject.description || '暂无描述'}
          </p>
          <p>
            <strong>Base URL：</strong>
            {currentProject.baseUrl || '未设置'}
          </p>
        </Card>

        <div style={{ marginBottom: 16 }}>
          <Button
            icon={<PlusOutlined />}
            type="primary"
            onClick={openCreateDocumentModal}
          >
            新建文档
          </Button>
        </div>

        <h4 style={{ marginBottom: 16 }}>
          <FileTextOutlined /> 文档列表
        </h4>

        <List
          dataSource={documents}
          loading={loadingDocuments}
          locale={{
            emptyText: (
              <div style={{ padding: 24, textAlign: 'center' }}>
                <span style={{ color: '#999' }}>
                  暂无文档，请点击上方"新建文档"按钮创建
                </span>
              </div>
            ),
          }}
          renderItem={(document: any) => (
            <List.Item
              actions={[
                <Button
                  icon={<EditOutlined />}
                  key="edit"
                  onClick={(e) => handleEditDocument(document, e)}
                  type="link"
                >
                  编辑
                </Button>,
                <Popconfirm
                  key="delete"
                  cancelText="取消"
                  description="删除后不可恢复，文档下的所有接口都将被删除"
                  okButtonProps={{ danger: true }}
                  okText="删除"
                  title="确定要删除这个文档吗？"
                  onConfirm={(e) =>
                    handleDeleteDocument(document, e as unknown as React.MouseEvent)
                  }
                >
                  <Button danger icon={<DeleteOutlined />} type="link">
                    删除
                  </Button>
                </Popconfirm>,
                <Button
                  icon={<RightOutlined />}
                  key="enter"
                  onClick={(e) => {
                    e.stopPropagation();
                    handleSelectDocument(document);
                  }}
                  type="link"
                >
                  进入
                </Button>,
              ]}
            >
              <List.Item.Meta
                description={document.description || '暂无描述'}
                title={document.name}
              />
            </List.Item>
          )}
        />

        <Card style={{ marginTop: 24 }}>
          <h5 style={{ marginBottom: 16 }}>
            <SettingOutlined /> 系统设置
          </h5>
          <Button
            block
            icon={<DatabaseOutlined />}
            onClick={() => setCurrentPage('globalParameters')}
            style={{ marginBottom: 8 }}
          >
            全局参数管理
          </Button>
        </Card>
      </div>
    );
  };

  const userMenuItems = [
    {
      key: 'username',
      label: <span>当前用户：{user?.username || '未知'}</span>,
    },
    { type: 'divider' as const },
    {
      danger: true,
      key: 'logout',
      label: '退出登录',
    },
  ];

  const projectMenuItems = projects.map((project) => ({
    key: project.id,
    label: (
      <div
        onClick={(e) => {
          e.stopPropagation();
          handleSelectProject(project);
          setProjectsOpen(false);
        }}
        style={{
          alignItems: 'center',
          display: 'flex',
          justifyContent: 'space-between',
          padding: '4px 0',
        }}
      >
        <span>{project.name}</span>
        <Space size="small">
          <Button
            icon={<EditOutlined />}
            size="small"
            type="text"
            onClick={(e) => {
              e.stopPropagation();
              handleEditProject(project, e);
            }}
          />
          <Popconfirm
            cancelText="取消"
            okButtonProps={{ danger: true }}
            okText="删除"
            title="确定要删除这个项目吗？"
            onConfirm={(e) => handleDeleteProject(project, e as unknown as React.MouseEvent)}
          >
            <Button danger icon={<DeleteOutlined />} size="small" type="text" />
          </Popconfirm>
          {currentProject?.id === project.id && (
            <Tag color="blue">当前</Tag>
          )}
        </Space>
      </div>
    ),
  }));

  const siderContent = (
    <div style={{ padding: '16px 0' }}>
      <Dropdown
        menu={{ items: projectMenuItems }}
        onOpenChange={setProjectsOpen}
        open={projectsOpen}
        overlayStyle={{ maxHeight: 400, overflow: 'auto', width: 350 }}
        trigger={['click']}
      >
        <div
          style={{
            alignItems: 'center',
            background: projectsOpen ? '#e6f7ff' : 'transparent',
            cursor: 'pointer',
            display: 'flex',
            justifyContent: 'space-between',
            padding: '12px 24px',
            transition: 'all 0.3s',
          }}
        >
          <Space>
            {projectsOpen ? <FolderOpenOutlined /> : <FolderOutlined />}
            <span>项目</span>
          </Space>
          <PlusOutlined
            onClick={(e) => {
              e.stopPropagation();
              e.preventDefault();
              openCreateProjectModal();
            }}
          />
        </div>
      </Dropdown>

      <div style={{ borderTop: '1px solid #f0f0f0', padding: '8px 24px' }}>
        <span style={{ color: '#999', fontSize: 12 }}>
          已创建 {projects.length} 个项目
        </span>
      </div>

      {currentDocument && (
        <>
          <div style={{ borderTop: '1px solid #f0f0f0', padding: '16px 24px 8px' }}>
            <span style={{ color: '#999', fontSize: 12 }}>当前文档</span>
          </div>
          <div
            onClick={() => setCurrentPage('endpoints')}
            style={{
              alignItems: 'center',
              background: currentPage === 'endpoints' ? '#e6f7ff' : 'transparent',
              cursor: 'pointer',
              display: 'flex',
              padding: '8px 24px',
            }}
          >
            <FileTextOutlined style={{ marginRight: 8 }} />
            <span
              style={{
                overflow: 'hidden',
                textOverflow: 'ellipsis',
                whiteSpace: 'nowrap',
              }}
            >
              {currentDocument.name}
            </span>
          </div>
        </>
      )}

      {currentDocument && (
        <>
          <div style={{ borderTop: '1px solid #f0f0f0', padding: '16px 24px 8px' }}>
            <span style={{ color: '#999', fontSize: 12 }}>功能菜单</span>
          </div>
          <div
            onClick={() => setCurrentPage('endpoints')}
            style={{
              alignItems: 'center',
              background: currentPage === 'endpoints' ? '#e6f7ff' : 'transparent',
              cursor: 'pointer',
              display: 'flex',
              padding: '8px 24px',
            }}
          >
            <ApiOutlined style={{ marginRight: 8 }} />
            <span>接口列表</span>
          </div>
          <div
            onClick={() => setCurrentPage('parameterTemplates')}
            style={{
              alignItems: 'center',
              background:
                currentPage === 'parameterTemplates' ? '#e6f7ff' : 'transparent',
              color: currentPage === 'parameterTemplates' ? '#1890ff' : '#000',
              cursor: 'pointer',
              display: 'flex',
              padding: '8px 24px 8px 40px',
            }}
          >
            <DatabaseOutlined style={{ marginRight: 8 }} />
            <span>参数模板</span>
          </div>
          <div
            onClick={() => setCurrentPage('testcases')}
            style={{
              alignItems: 'center',
              background: currentPage === 'testcases' ? '#e6f7ff' : 'transparent',
              cursor: 'pointer',
              display: 'flex',
              padding: '8px 24px',
            }}
          >
            <ExperimentOutlined style={{ marginRight: 8 }} />
            <span>测试用例</span>
          </div>
          <div
            onClick={() => setCurrentPage('export')}
            style={{
              alignItems: 'center',
              background: currentPage === 'export' ? '#e6f7ff' : 'transparent',
              cursor: 'pointer',
              display: 'flex',
              padding: '8px 24px',
            }}
          >
            <ExportOutlined style={{ marginRight: 8 }} />
            <span>导出</span>
          </div>
        </>
      )}
    </div>
  );

  if (!isAuthenticated) {
    return (
      <BrowserRouter>
        <Routes>
          <Route element={<LoginPage />} path="/login" />
          <Route element={<Navigate replace to="/login" />} path="*" />
        </Routes>
      </BrowserRouter>
    );
  }

  return (
    <BrowserRouter>
      <Layout style={{ minHeight: '100vh' }}>
        <Header
          style={{
            alignItems: 'center',
            background: '#001529',
            display: 'flex',
            justifyContent: 'space-between',
            padding: '0 24px',
          }}
        >
          <div
            style={{
              alignItems: 'center',
              color: 'white',
              display: 'flex',
              fontSize: 18,
              fontWeight: 'bold',
            }}
          >
            <ApiOutlined style={{ marginRight: 8 }} />
            API Document Manager
          </div>
          <Space>
            <Dropdown
              menu={{
                items: userMenuItems,
                onClick: ({ key }) => {
                  if (key === 'logout') {
                    handleLogout();
                  }
                },
              }}
              trigger={['click']}
            >
              <Space style={{ color: 'white', cursor: 'pointer' }}>
                <UserOutlined />
                <span>{user?.username || 'User'}</span>
              </Space>
            </Dropdown>
            <Button
              icon={<LogoutOutlined />}
              onClick={handleLogout}
              style={{ color: 'white' }}
              type="text"
            />
          </Space>
        </Header>
        <Layout style={{ height: 'calc(100vh - 64px)' }}>
          <Sider
            style={{
              background: '#fff',
              borderRight: '1px solid #f0f0f0',
              overflow: 'auto',
              width: '30%',
            }}
          >
            {siderContent}
          </Sider>
          <Content
            style={{
              background: '#f5f5f5',
              overflow: 'auto',
              padding: 0,
            }}
          >
            {currentPage === 'endpoints' ? (
              currentDocument ? (
                <EndpointEditor />
              ) : (
                renderEmptyDocumentHint()
              )
            ) : currentPage === 'testcases' ? (
              currentDocument ? (
                <TestCaseGenerator />
              ) : (
                renderEmptyDocumentHint()
              )
            ) : currentPage === 'export' ? (
              currentDocument ? (
                <ExportPanel />
              ) : (
                renderEmptyDocumentHint()
              )
            ) : currentPage === 'globalParameters' ? (
              <GlobalParameterPanel />
            ) : currentPage === 'parameterTemplates' ? (
              <ParameterTemplatePanel />
            ) : (
              renderMainContent()
            )}
          </Content>
        </Layout>
      </Layout>

      <Modal
        footer={null}
        onCancel={() => {
          setCreateDocModalOpen(false);
          setEditingDocument(null);
          void createDocForm.resetFields();
        }}
        open={createDocModalOpen}
        title={editingDocument ? '编辑文档' : '新建文档'}
      >
        <Form
          form={createDocForm}
          layout="vertical"
          onFinish={handleCreateDocument}
        >
          <Form.Item
            label="文档名称"
            name="name"
            rules={[{ message: '请输入文档名称', required: true }]}
          >
            <Input placeholder="请输入文档名称" />
          </Form.Item>
          <Form.Item label="描述" name="description">
            <TextArea placeholder="请输入文档描述" rows={3} />
          </Form.Item>
          <Form.Item label="版本号" name="version">
            <Input placeholder="1.0.0" />
          </Form.Item>
          <Form.Item>
            <Space>
              <Button htmlType="submit" type="primary">
                {editingDocument ? '更新' : '创建'}
              </Button>
              <Button
                onClick={() => {
                  setCreateDocModalOpen(false);
                  setEditingDocument(null);
                  void createDocForm.resetFields();
                }}
              >
                取消
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        footer={null}
        onCancel={() => {
          setCreateProjectModalOpen(false);
          setEditingProject(null);
          void createProjectForm.resetFields();
        }}
        open={createProjectModalOpen}
        title={editingProject ? '编辑项目' : '新建项目'}
      >
        <Form
          form={createProjectForm}
          layout="vertical"
          onFinish={handleCreateProject}
        >
          <Form.Item
            label="项目名称"
            name="name"
            rules={[{ message: '请输入项目名称', required: true }]}
          >
            <Input placeholder="请输入项目名称" />
          </Form.Item>
          <Form.Item label="描述" name="description">
            <TextArea placeholder="请输入项目描述" rows={3} />
          </Form.Item>
          <Form.Item label="Base URL" name="baseUrl">
            <Input placeholder="https://api.example.com" />
          </Form.Item>
          <Form.Item label="版本号" name="version">
            <Input placeholder="1.0.0" />
          </Form.Item>
          <Form.Item>
            <Space>
              <Button htmlType="submit" type="primary">
                {editingProject ? '更新' : '创建'}
              </Button>
              <Button
                onClick={() => {
                  setCreateProjectModalOpen(false);
                  setEditingProject(null);
                  void createProjectForm.resetFields();
                }}
              >
                取消
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>
    </BrowserRouter>
  );
};

export default App;
