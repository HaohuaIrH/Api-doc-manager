import React, { useState, useEffect } from 'react';
import {
  Table, Button, Modal, Form, Input, Select, Switch, Space, Popconfirm, message
} from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, DatabaseOutlined } from '@ant-design/icons';
import { parameterApi } from '../services/api';
import ParameterSelector from '../components/ParameterSelector';
import { createDataTypeValidator, validateParameterValue } from '../utils/parameterValidator';

const PARAM_TYPES = [
  { value: 'HEADER', label: 'Header' },
  { value: 'PATH', label: 'Path' },
  { value: 'QUERY', label: 'Query' },
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

const ParameterEditor: React.FC<{ endpoint: any }> = ({ endpoint }) => {
  const [parameters, setParameters] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingParam, setEditingParam] = useState<any>(null);
  const [form] = Form.useForm();
  const [selectorVisible, setSelectorVisible] = useState(false);
  const [selectedDataType, setSelectedDataType] = useState<string>('string');

  const dataType = Form.useWatch('dataType', form);

  useEffect(() => {
    if (endpoint?.id) {
      fetchParameters();
    }
  }, [endpoint]);

  const fetchParameters = async () => {
    if (!endpoint?.id) return;
    setLoading(true);
    try {
      const res = await parameterApi.list(endpoint.id);
      setParameters(res.data);
    } catch (error) {
      message.error('加载参数列表失败');
    } finally {
      setLoading(false);
    }
  };

  const handleCreate = () => {
    setEditingParam(null);
    form.resetFields();
    setSelectedDataType('string');
    setModalVisible(true);
  };

  const handleEdit = (record: any) => {
    setEditingParam(record);
    form.setFieldsValue({
      ...record,
      enumValues: record.enumValues?.join(', '),
      required: record.required ?? false,
    });
    setSelectedDataType(record.dataType || 'string');
    setModalVisible(true);
  };

  const handleDelete = async (id: number) => {
    try {
      await parameterApi.delete(id);
      message.success('删除参数成功');
      fetchParameters();
    } catch (error) {
      message.error('删除参数失败');
    }
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();

      // 验证示例值
      const example = values.example || '';
      const dataType = values.dataType || 'string';
      const validation = validateParameterValue(example, dataType as any);

      if (!validation.valid) {
        message.error('请输入正确参数类型再尝试创建');
        return; // 阻止提交
      }

      const data = {
        ...values,
        endpointId: endpoint.id,
        enumValues: values.enumValues?.split(',').map((e: string) => e.trim()) || [],
        required: values.required ?? false,
      };
      if (editingParam) {
        await parameterApi.update(editingParam.id, data);
        message.success('更新参数成功');
      } else {
        await parameterApi.create(data);
        message.success('创建参数成功');
      }
      setModalVisible(false);
      fetchParameters();
    } catch (error) {
      message.error('操作失败');
    }
  };

  const columns = [
    { title: '名称', dataIndex: 'name', key: 'name' },
    { title: '位置', dataIndex: 'location', key: 'location' },
    { title: '类型', dataIndex: 'dataType', key: 'dataType' },
    { title: '必填', dataIndex: 'required', key: 'required', render: (v: boolean) => v ? '是' : '否' },
    { title: '描述', dataIndex: 'description', key: 'description' },
    {
      title: '操作',
      key: 'actions',
      render: (_: any, record: any) => (
        <Space>
          <Button type="link" icon={<EditOutlined />} onClick={() => handleEdit(record)} />
          <Popconfirm title="确定删除此参数？" onConfirm={() => handleDelete(record.id)}>
            <Button type="link" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <div style={{ marginBottom: 16, display: 'flex', gap: 8 }}>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
          新建参数
        </Button>
        <Button icon={<DatabaseOutlined />} onClick={() => setSelectorVisible(true)}>
          从全局参数选择
        </Button>
      </div>
      <Table dataSource={parameters} columns={columns} rowKey="id" loading={loading} />

      <Modal
        title={editingParam ? '编辑参数' : '新建参数'}
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={() => setModalVisible(false)}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="参数名称" rules={[{ required: true }]}>
            <Input placeholder="参数名称" />
          </Form.Item>
          <Form.Item name="location" label="参数位置" rules={[{ required: true }]}>
            <Select options={PARAM_TYPES} />
          </Form.Item>
          <Form.Item name="dataType" label="数据类型" rules={[{ required: true }]}>
            <Select options={DATA_TYPES} />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item name="required" label="必填" valuePropName="checked">
            <Switch />
          </Form.Item>
          <Form.Item
            noStyle
            shouldUpdate={(prev, curr) => prev.dataType !== curr.dataType}
          >
            {() => (
              <Form.Item
                name="example"
                label="示例值"
                rules={createDataTypeValidator(form.getFieldValue('dataType') || 'string')}
              >
                <Input placeholder="请输入符合数据类型的示例值" />
              </Form.Item>
            )}
          </Form.Item>
        </Form>
      </Modal>

      <ParameterSelector
        visible={selectorVisible}
        onClose={() => setSelectorVisible(false)}
        onSelect={(param) => {
          setSelectorVisible(false);
          setModalVisible(true);
          form.setFieldsValue(param);
        }}
      />
    </div>
  );
};

export default ParameterEditor;
