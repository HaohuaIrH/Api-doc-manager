import React, { useState, useEffect } from 'react';
import { Card, Button, Space, Modal, Form, Input, Select, message, Popconfirm } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, DatabaseOutlined, ArrowLeftOutlined } from '@ant-design/icons';
import ParameterBlock from '../components/ParameterBlock';
import ParameterSelector from '../components/ParameterSelector';
import { globalParameterApi } from '../services/api';
import { useAppStore } from '../stores/appStore';
import { createDataTypeValidator, validateParameterValue, DataType } from '../utils/parameterValidator';

const { Option } = Select;

interface GlobalParameter {
  id?: number;
  name: string;
  dataType: string;
  exampleValue?: string;
  description?: string;
  parentId?: number;
  sortOrder?: number;
  children?: GlobalParameter[];
}

const GlobalParameterPanel: React.FC = () => {
  const { setCurrentPage } = useAppStore();
  const [loading, setLoading] = useState(false);
  const [parameters, setParameters] = useState<GlobalParameter[]>([]);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingParameter, setEditingParameter] = useState<GlobalParameter | null>(null);
  const [form] = Form.useForm();
  const [selectorVisible, setSelectorVisible] = useState(false);
  const [selectedDataType, setSelectedDataType] = useState<string>('STRING');

  const dataType = Form.useWatch('dataType', form);

  useEffect(() => {
    loadParameters();
  }, []);

  const loadParameters = async () => {
    setLoading(true);
    try {
      const res = await globalParameterApi.list();
      const processedData = (res.data || []).map((param: any) => ({
        ...param,
        dataType: typeof param.dataType === 'object' ? param.dataType.name : param.dataType,
      }));
      setParameters(processedData);
    } catch (error) {
      message.error('加载参数失败');
    } finally {
      setLoading(false);
    }
  };

  const convertDataType = (type: string): DataType => {
    const typeMap: Record<string, DataType> = {
      'STRING': 'string',
      'INTEGER': 'integer',
      'LONG': 'integer',
      'DOUBLE': 'number',
      'NUMBER': 'number',
      'BOOLEAN': 'boolean',
      'ARRAY': 'array',
      'OBJECT': 'object',
    };
    return typeMap[type.toUpperCase()] || 'string';
  };

  const handleCreate = () => {
    setEditingParameter(null);
    form.resetFields();
    setSelectedDataType('STRING');
    setModalVisible(true);
  };

  const handleEdit = (record: any) => {
    setEditingParameter(record);
    const dataType = record.dataType || 'STRING';
    setSelectedDataType(dataType);
    form.setFieldsValue({
      name: record.name,
      dataType: dataType,
      exampleValue: record.exampleValue,
      description: record.description
    });
    setModalVisible(true);
  };

  const handleDelete = async (id: number) => {
    try {
      await globalParameterApi.delete(id);
      message.success('删除成功');
      loadParameters();
    } catch (error) {
      message.error('删除失败');
    }
  };

  const handleSubmit = async (values: any) => {
    try {
      // 验证示例值
      const exampleValue = values.exampleValue || '';
      const dataType = values.dataType || 'STRING';
      const validation = validateParameterValue(exampleValue, convertDataType(dataType));

      if (!validation.valid) {
        message.error('请输入正确参数类型再尝试创建');
        return; // 阻止提交
      }

      if (editingParameter) {
        await globalParameterApi.update(editingParameter.id!, values);
        message.success('更新成功');
      } else {
        await globalParameterApi.create(values);
        message.success('创建成功');
      }
      setModalVisible(false);
      loadParameters();
    } catch (error: any) {
      const errorMsg = error.response?.data?.message || error.message || '保存失败';

      if (errorMsg.includes('参数名称已存在')) {
        message.error('参数名称已存在，请使用其他名称');
      } else if (errorMsg.includes('参数名称不能为空')) {
        message.error('参数名称不能为空');
      } else {
        message.error(errorMsg);
      }
    }
  };

  const openSelector = () => {
    setSelectorVisible(true);
  };

  const handleSelectParameter = (parameter: any) => {
    const processedParam = {
      ...parameter,
      dataType: typeof parameter.dataType === 'object' ? parameter.dataType.name : parameter.dataType,
    };
    setEditingParameter(processedParam);
    form.setFieldsValue({
      name: processedParam.name,
      dataType: processedParam.dataType,
      exampleValue: processedParam.exampleValue,
      description: processedParam.description
    });
    setModalVisible(true);
  };

  const renderParameterBlocks = (params: GlobalParameter[]) => {
    if (!params || params.length === 0) return null;
    
    return params.map((param) => (
      <div key={param.id} style={{ marginBottom: 16 }}>
        <ParameterBlock
          parameter={param}
          onClick={() => handleEdit(param)}
          showDelete
          onDelete={handleDelete}
        />
      </div>
    ));
  };

  return (
    <div style={{ padding: 16 }}>
      <Card
        title={
          <Space>
            <DatabaseOutlined />
            <span>全局参数库</span>
          </Space>
        }
        extra={
          <Space>
            <Button icon={<ArrowLeftOutlined />} onClick={() => setCurrentPage('home')}>
              返回
            </Button>
            <Button icon={<PlusOutlined />} onClick={handleCreate}>
              新建参数
            </Button>
          </Space>
        }
      >
        <div style={{ marginBottom: 16 }}>
          <Button onClick={openSelector}>
            从全局参数选择
          </Button>
        </div>

        {renderParameterBlocks(parameters)}
      </Card>

      <Modal
        title={editingParameter ? '编辑参数' : '新建参数'}
        open={modalVisible}
        onOk={() => form.submit()}
        onCancel={() => setModalVisible(false)}
      >
        <Form form={form} layout="vertical" onFinish={handleSubmit}>
          <Form.Item name="name" label="参数名称" rules={[{ required: true, message: '请输入参数名称' }]}>
            <Input placeholder="参数名称" />
          </Form.Item>

          <Form.Item name="dataType" label="数据类型" rules={[{ required: true, message: '请选择数据类型' }]}>
            <Select placeholder="请选择数据类型">
              <Option value="STRING">STRING</Option>
              <Option value="INTEGER">INTEGER</Option>
              <Option value="LONG">LONG</Option>
              <Option value="DOUBLE">DOUBLE</Option>
              <Option value="BOOLEAN">BOOLEAN</Option>
              <Option value="ARRAY">ARRAY</Option>
              <Option value="OBJECT">OBJECT</Option>
            </Select>
          </Form.Item>

          <Form.Item
            noStyle
            shouldUpdate={(prev, curr) => prev.dataType !== curr.dataType}
          >
            {() => {
              const currentDataType = form.getFieldValue('dataType') || 'STRING';
              const validatorDataType = convertDataType(currentDataType);
              return (
                <Form.Item
                  name="exampleValue"
                  label="示例值"
                  rules={createDataTypeValidator(validatorDataType)}
                >
                  <Input placeholder="请输入符合数据类型的示例值" />
                </Form.Item>
              );
            }}
          </Form.Item>

          <Form.Item name="description" label="描述">
            <Input.TextArea rows={3} placeholder="参数描述" />
          </Form.Item>

          <Form.Item name="sortOrder" label="排序">
            <Input type="number" min={0} defaultValue={0} />
          </Form.Item>

          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit">
                {editingParameter ? '更新' : '创建'}
              </Button>
              <Button onClick={() => setModalVisible(false)}>取消</Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>

      <ParameterSelector
        visible={selectorVisible}
        onClose={() => setSelectorVisible(false)}
        onSelect={handleSelectParameter}
      />
    </div>
  );
};

export default GlobalParameterPanel;
