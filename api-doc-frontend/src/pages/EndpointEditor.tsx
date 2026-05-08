import React, { useState, useEffect } from 'react';
import {
  Table, Button, Modal, Form, Input, Select, Space, Popconfirm, message, Tag, Divider, Switch
} from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, BranchesOutlined, DownOutlined, UpOutlined, SaveOutlined, FolderOpenOutlined } from '@ant-design/icons';
import { endpointApi, parameterTemplateApi } from '../services/api';
import { useAppStore } from '../stores/appStore';
import ParameterTemplateSelector from '../components/ParameterTemplateSelector';
import { validateParameterValue, validateRange, validateLength, DataType } from '../utils/parameterValidator';

const HTTP_METHODS = ['GET', 'POST', 'PUT', 'DELETE', 'PATCH', 'OPTIONS', 'HEAD'];
const METHOD_COLORS: Record<string, string> = {
  GET: 'green',
  POST: 'blue',
  PUT: 'orange',
  DELETE: 'red',
  PATCH: 'purple',
  OPTIONS: 'cyan',
  HEAD: 'gray',
};

const PARAM_LOCATIONS = [
  { value: 'QUERY', label: 'Query' },
  { value: 'PATH', label: 'Path' },
  { value: 'HEADER', label: 'Header' },
  { value: 'REQUEST_BODY', label: 'Request Body' },
  { value: 'RESPONSE_BODY', label: 'Response Body' },
];

const DATA_TYPES = [
  { value: 'string', label: 'string' },
  { value: 'integer', label: 'integer' },
  { value: 'number', label: 'number' },
  { value: 'boolean', label: 'boolean' },
  { value: 'array', label: 'array' },
  { value: 'object', label: 'object' },
];

interface ParamItem {
  id?: number;
  key: string;
  location: string;
  name: string;
  description: string;
  required: boolean;
  dataType: string;
  format: string;
  defaultValue?: string;
  example?: string;
  minLength?: number;
  maxLength?: number;
  minimum?: number;
  maximum?: number;
}

const EndpointEditor: React.FC = () => {
  const { currentDocument, endpoints, setEndpoints, setCurrentPage } = useAppStore();
  const [loading, setLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingEndpoint, setEditingEndpoint] = useState<any>(null);
  const [form] = Form.useForm();
  const [paramsExpanded, setParamsExpanded] = useState(false);
  const [templateSelectorVisible, setTemplateSelectorVisible] = useState(false);
  const [savingTemplate, setSavingTemplate] = useState(false);
  const [parameters, setParameters] = useState<ParamItem[]>([]);

  useEffect(() => {
    if (currentDocument?.id) {
      fetchEndpoints();
    }
  }, [currentDocument]);

  const fetchEndpoints = async () => {
    if (!currentDocument?.id) return;
    setLoading(true);
    try {
      const res = await endpointApi.list(currentDocument.id);
      setEndpoints(res.data);
    } catch (error) {
      message.error('加载接口列表失败');
    } finally {
      setLoading(false);
    }
  };

  const handleCreate = () => {
    setEditingEndpoint(null);
    form.resetFields();
    setParameters([]);
    setParamsExpanded(false);
    setModalVisible(true);
  };

  const handleEdit = (record: any) => {
    setEditingEndpoint(record);
    form.setFieldsValue({
      ...record,
    });
    const params = (record.parameters || []).map((p: any, idx: number) => ({
      ...p,
      key: p.id || `new_${idx}`,
    }));
    setParameters(params);
    setParamsExpanded(params.length > 0);
    setModalVisible(true);
  };

  const handleDelete = async (id: number) => {
    try {
      await endpointApi.delete(id);
      message.success('删除接口成功');
      fetchEndpoints();
    } catch (error) {
      message.error('删除接口失败');
    }
  };

  const handleGoToTestCase = () => {
    setCurrentPage('testcases');
  };

  const handleAddParam = () => {
    const newParam: ParamItem = {
      key: `new_${Date.now()}`,
      location: 'QUERY',
      name: '',
      description: '',
      required: false,
      dataType: 'string',
      format: '',
      defaultValue: '',
      example: '',
      minLength: undefined,
      maxLength: undefined,
      minimum: undefined,
      maximum: undefined,
    };
    setParameters([...parameters, newParam]);
    setParamsExpanded(true);
  };

  const handleDeleteParam = (key: string) => {
    setParameters(parameters.filter(p => p.key !== key));
  };

  const handleParamChange = (key: string, field: string, value: any) => {
    setParameters(parameters.map(p => 
      p.key === key ? { ...p, [field]: value } : p
    ));
  };

  const handleSaveAsTemplate = async () => {
    if (!currentDocument?.id) {
      message.error('请先选择文档');
      return;
    }

    if (parameters.length === 0) {
      message.error('请先添加参数');
      return;
    }

    try {
      setSavingTemplate(true);
      const values = await form.validateFields();
      const folderName = values.summary || values.path;

      if (!folderName || folderName.trim() === '') {
        message.error('请先填写接口名称或路径');
        return;
      }

      const templateData = {
        folderName: folderName.trim(),
        templateName: folderName.trim() + ' 参数模板',
        parameters: parameters.map(p => ({
          name: p.name,
          location: p.location,
          dataType: p.dataType,
          description: p.description,
          required: p.required,
          example: p.example,
        })),
        documentId: currentDocument.id,
      };

      await parameterTemplateApi.create(templateData);
      message.success('模板保存成功');
    } catch (error: any) {
      console.error('保存模板失败:', error);
      const errorMsg = error.response?.data?.message || error.message || '保存模板失败';
      message.error(errorMsg);
    } finally {
      setSavingTemplate(false);
    }
  };

  const handleSelectTemplate = (template: any) => {
    if (template.parameters && Array.isArray(template.parameters)) {
      const newParams = template.parameters.map((p: any, idx: number) => ({
        key: `template_${idx}_${Date.now()}`,
        location: p.location || 'QUERY',
        name: p.name || '',
        description: p.description || '',
        required: p.required || false,
        dataType: p.dataType || 'string',
        format: '',
        defaultValue: '',
        example: p.example || '',
      }));
      setParameters(newParams);
      setParamsExpanded(true);
      message.success('已加载模板参数');
      setTemplateSelectorVisible(false);
    }
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();

      // 验证所有参数的类型
      const invalidParams = parameters.filter(p => {
        if (!p.name || !p.name.trim()) return false; // 跳过没有名称的参数
        const exampleValidation = validateParameterValue(p.example || '', p.dataType as DataType);
        const defaultValidation = validateParameterValue(p.defaultValue || '', p.dataType as DataType);
        return !exampleValidation.valid || !defaultValidation.valid;
      });

      if (invalidParams.length > 0) {
        message.error('请输入正确参数类型再尝试创建');
        return; // 阻止提交
      }

      const validParams = parameters.filter(p => p.name && p.name.trim());

      const data = {
        ...values,
        documentId: currentDocument?.id,
        parameters: validParams.map(p => ({
          location: p.location,
          name: p.name,
          description: p.description,
          required: p.required,
          dataType: p.dataType,
          format: p.format,
          defaultValue: p.defaultValue,
          example: p.example,
          minLength: p.minLength,
          maxLength: p.maxLength,
          minimum: p.minimum,
          maximum: p.maximum,
        })),
      };

      if (editingEndpoint) {
        await endpointApi.update(editingEndpoint.id, data);
        message.success('接口更新成功');
      } else {
        await endpointApi.create(data);
        message.success('接口创建成功');
      }
      setModalVisible(false);
      fetchEndpoints();
    } catch (error: any) {
      message.error(error.response?.data?.error || '操作失败');
    }
  };

  const paramColumns = [
    {
      title: '位置',
      dataIndex: 'location',
      key: 'location',
      width: 100,
      render: (location: string, record: ParamItem) => (
        <Select
          size="small"
          value={location}
          style={{ width: '100%' }}
          options={PARAM_LOCATIONS}
          onChange={(val) => handleParamChange(record.key, 'location', val)}
        />
      ),
    },
    {
      title: '名称',
      dataIndex: 'name',
      key: 'name',
      width: 100,
      render: (name: string, record: ParamItem) => (
        <Input
          size="small"
          value={name}
          placeholder="参数名"
          onChange={(e) => handleParamChange(record.key, 'name', e.target.value)}
        />
      ),
    },
    {
      title: '类型',
      dataIndex: 'dataType',
      key: 'dataType',
      width: 90,
      render: (dataType: string, record: ParamItem) => (
        <Select
          size="small"
          value={dataType}
          style={{ width: '100%' }}
          options={DATA_TYPES}
          onChange={(val) => handleParamChange(record.key, 'dataType', val)}
        />
      ),
    },
    {
      title: '必填',
      dataIndex: 'required',
      key: 'required',
      width: 70,
      render: (required: boolean, record: ParamItem) => (
        <Switch
          size="small"
          checked={required}
          onChange={(checked) => handleParamChange(record.key, 'required', checked)}
        />
      ),
    },
    {
      title: '示例',
      dataIndex: 'example',
      key: 'example',
      width: 100,
      render: (example: string, record: ParamItem) => {
        const validation = validateParameterValue(example || '', record.dataType as DataType);
        const hasError = !validation.valid;
        return (
          <div>
            <Input
              size="small"
              value={example}
              placeholder="示例值"
              status={hasError ? 'error' : undefined}
              title={hasError ? validation.message : undefined}
              onChange={(e) => handleParamChange(record.key, 'example', e.target.value)}
            />
            {hasError && (
              <div style={{ color: '#ff4d4f', fontSize: '10px', marginTop: 2 }}>
                {validation.message}
              </div>
            )}
          </div>
        );
      },
    },
    {
      title: '默认',
      dataIndex: 'defaultValue',
      key: 'defaultValue',
      width: 100,
      render: (defaultValue: string, record: ParamItem) => {
        const validation = validateParameterValue(defaultValue || '', record.dataType as DataType);
        const hasError = !validation.valid;
        return (
          <div>
            <Input
              size="small"
              value={defaultValue}
              placeholder="默认值"
              status={hasError ? 'error' : undefined}
              title={hasError ? validation.message : undefined}
              onChange={(e) => handleParamChange(record.key, 'defaultValue', e.target.value)}
            />
            {hasError && (
              <div style={{ color: '#ff4d4f', fontSize: '10px', marginTop: 2 }}>
                {validation.message}
              </div>
            )}
          </div>
        );
      },
    },
    {
      title: '长度限制',
      key: 'lengthConstraints',
      width: 150,
      render: (_: any, record: ParamItem) => {
        const example = record.example || '';
        const minLengthValidation = record.minLength !== undefined
          ? validateLength(example, record.minLength, undefined)
          : { valid: true };
        const maxLengthValidation = record.maxLength !== undefined
          ? validateLength(example, undefined, record.maxLength)
          : { valid: true };
        const hasMinError = !minLengthValidation.valid;
        const hasMaxError = !maxLengthValidation.valid;

        return (
          <Space size="small">
            <div>
              <Input
                size="small"
                type="number"
                placeholder="最小"
                style={{ width: 60 }}
                value={record.minLength}
                status={hasMinError ? 'error' : undefined}
                title={hasMinError ? minLengthValidation.message : undefined}
                onChange={(e) => handleParamChange(record.key, 'minLength', e.target.value ? parseInt(e.target.value) : undefined)}
              />
            </div>
            <span>/</span>
            <div>
              <Input
                size="small"
                type="number"
                placeholder="最大"
                style={{ width: 60 }}
                value={record.maxLength}
                status={hasMaxError ? 'error' : undefined}
                title={hasMaxError ? maxLengthValidation.message : undefined}
                onChange={(e) => handleParamChange(record.key, 'maxLength', e.target.value ? parseInt(e.target.value) : undefined)}
              />
            </div>
          </Space>
        );
      },
    },
    {
      title: '数值限制',
      key: 'valueConstraints',
      width: 150,
      render: (_: any, record: ParamItem) => {
        const exampleNum = record.example ? parseFloat(record.example) : undefined;
        const minValidation = record.minimum !== undefined && exampleNum !== undefined && !isNaN(exampleNum)
          ? validateRange(exampleNum, record.minimum, undefined)
          : { valid: true };
        const maxValidation = record.maximum !== undefined && exampleNum !== undefined && !isNaN(exampleNum)
          ? validateRange(exampleNum, undefined, record.maximum)
          : { valid: true };
        const hasMinError = !minValidation.valid;
        const hasMaxError = !maxValidation.valid;

        return (
          <Space size="small">
            <div>
              <Input
                size="small"
                type="number"
                placeholder="最小"
                style={{ width: 60 }}
                value={record.minimum}
                status={hasMinError ? 'error' : undefined}
                title={hasMinError ? minValidation.message : undefined}
                onChange={(e) => handleParamChange(record.key, 'minimum', e.target.value ? parseFloat(e.target.value) : undefined)}
              />
            </div>
            <span>/</span>
            <div>
              <Input
                size="small"
                type="number"
                placeholder="最大"
                style={{ width: 60 }}
                value={record.maximum}
                status={hasMaxError ? 'error' : undefined}
                title={hasMaxError ? maxValidation.message : undefined}
                onChange={(e) => handleParamChange(record.key, 'maximum', e.target.value ? parseFloat(e.target.value) : undefined)}
              />
            </div>
          </Space>
        );
      },
    },
    {
      title: '描述',
      dataIndex: 'description',
      key: 'description',
      render: (description: string, record: ParamItem) => (
        <Input
          size="small"
          value={description}
          placeholder="描述"
          onChange={(e) => handleParamChange(record.key, 'description', e.target.value)}
        />
      ),
    },
    {
      title: '',
      key: 'action',
      width: 50,
      render: (_: any, record: ParamItem) => (
        <Button
          type="text"
          danger
          size="small"
          icon={<DeleteOutlined />}
          onClick={() => handleDeleteParam(record.key)}
        />
      ),
    },
  ];

  const columns = [
    {
      title: '方法',
      dataIndex: 'method',
      key: 'method',
      width: 100,
      render: (method: string) => (
        <Tag color={METHOD_COLORS[method] || 'default'}>{method}</Tag>
      ),
    },
    { title: '路径', dataIndex: 'path', key: 'path', render: (path: string) => <code>{path}</code> },
    { title: '名称', dataIndex: 'summary', key: 'summary' },
    {
      title: '废弃',
      dataIndex: 'deprecated',
      key: 'deprecated',
      render: (deprecated: boolean) => (
        <Tag color={deprecated ? 'red' : 'green'}>
          {deprecated ? '是' : '否'}
        </Tag>
      ),
    },
    {
      title: '参数',
      key: 'paramsCount',
      width: 80,
      render: (_: any, record: any) => (
        <Tag color="blue">{record.parameters?.length || 0}</Tag>
      ),
    },
    {
      title: '操作',
      key: 'actions',
      width: 150,
      render: (_: any, record: any) => (
        <Space>
          <Button
            type="link"
            size="small"
            icon={<EditOutlined />}
            onClick={() => handleEdit(record)}
          />
          <Popconfirm
            title="确定删除此接口？"
            onConfirm={() => handleDelete(record.id)}
          >
            <Button type="link" size="small" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between' }}>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
          新建接口
        </Button>
        <Button icon={<BranchesOutlined />} onClick={handleGoToTestCase}>
          前往测试用例
        </Button>
      </div>
      <Table
        dataSource={endpoints}
        columns={columns}
        rowKey="id"
        loading={loading}
        pagination={false}
      />

      <Modal
        title={editingEndpoint ? '编辑接口' : '新建接口'}
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={() => setModalVisible(false)}
        width={950}
        bodyStyle={{ padding: '20px 24px' }}
      >
        <div style={{ width: '100%', minWidth: 850 }}>
          <Form form={form} layout="vertical" style={{ width: '100%' }}>
          <div style={{ display: 'flex', gap: 16, width: '100%' }}>
            <Form.Item name="method" label="方法" rules={[{ required: true }]} style={{ width: 200, marginBottom: 0 }}>
              <Select options={HTTP_METHODS.map((m) => ({ label: m, value: m }))} />
            </Form.Item>
            <Form.Item name="path" label="路径" rules={[{ required: true }]} style={{ flex: 1, marginBottom: 0 }}>
              <Input placeholder="/api/users/{id}" />
            </Form.Item>
          </div>
          <Form.Item name="summary" label="名称" rules={[{ required: true }]} style={{ width: '100%' }}>
            <Input placeholder="接口名称" />
          </Form.Item>
          <Form.Item name="description" label="描述" style={{ width: '100%' }}>
            <Input.TextArea rows={2} placeholder="接口详细描述" />
          </Form.Item>
          <div style={{ display: 'flex', gap: 16, width: '100%' }}>
            <Form.Item name="deprecated" label="废弃" style={{ width: 'auto', minWidth: 100, marginBottom: 0 }}>
              <Select
                options={[
                  { label: '否', value: false },
                  { label: '是', value: true },
                ]}
              />
            </Form.Item>
          </div>

          <Divider>
            <Button
              type="link"
              onClick={() => setParamsExpanded(!paramsExpanded)}
              icon={paramsExpanded ? <UpOutlined /> : <DownOutlined />}
            >
              参数列表 ({parameters.length})
            </Button>
          </Divider>

          {paramsExpanded && (
            <div style={{ marginBottom: 16 }}>
              <Space style={{ marginBottom: 8, width: '100%' }}>
                <Button
                  type="dashed"
                  icon={<PlusOutlined />}
                  onClick={handleAddParam}
                  style={{ flex: 1 }}
                >
                  添加参数
                </Button>
                <Button
                  icon={<SaveOutlined />}
                  onClick={handleSaveAsTemplate}
                  loading={savingTemplate}
                >
                  保存为模板
                </Button>
                <Button
                  icon={<FolderOpenOutlined />}
                  onClick={() => setTemplateSelectorVisible(true)}
                >
                  从模板加载
                </Button>
              </Space>
              <Table
                dataSource={parameters}
                columns={paramColumns}
                rowKey="key"
                size="small"
                pagination={false}
                style={{ marginTop: 8 }}
                scroll={{ x: 'max-content' }}
              />
            </div>
          )}
        </Form>
      </div>
      </Modal>

      <ParameterTemplateSelector
        visible={templateSelectorVisible}
        documentId={currentDocument?.id || 0}
        onClose={() => setTemplateSelectorVisible(false)}
        onSelect={handleSelectTemplate}
      />
    </div>
  );
};

export default EndpointEditor;