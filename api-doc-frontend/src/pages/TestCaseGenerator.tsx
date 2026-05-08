import React, { useState, useEffect } from 'react';
import { Table, Button, Space, message, Tag, Modal, Select, Card, Typography, Input, Form, Row, Col } from 'antd';
import { DownloadOutlined, ThunderboltOutlined, DatabaseOutlined, PlusOutlined, DeleteOutlined } from '@ant-design/icons';
import { testCaseApi, endpointApi } from '../services/api';
import { useAppStore } from '../stores/appStore';
import ParameterSelector from '../components/ParameterSelector';
import ParameterBlock from '../components/ParameterBlock';
import { validateParameterValue, DataType } from '../utils/parameterValidator';

const { Text } = Typography;

interface TestCase {
  id?: number;
  name: string;
  type: string;
  requestConfig: any;
  expectedResponse: any;
  testData: any;
}

interface ParamItem {
  name: string;
  required: boolean;
  dataType: string;
  example: string;
  location: string;
}

interface ChildParam {
  name: string;
  value: string;
}

interface ComplexParamValue {
  [key: string]: string | number | boolean | object;
}

const TestCaseGenerator: React.FC = () => {
  const { currentDocument, endpoints, setEndpoints } = useAppStore();
  const [selectedEndpointId, setSelectedEndpointId] = useState<number | null>(null);
  const [testCases, setTestCases] = useState<TestCase[]>([]);
  const [curlOutput, setCurlOutput] = useState<string>('');
  const [postmanOutput, setPostmanOutput] = useState<string>('');
  const [loading, setLoading] = useState(false);
  const [curlModalVisible, setCurlModalVisible] = useState(false);
  const [postmanModalVisible, setPostmanModalVisible] = useState(false);
  const [selectedEndpointParams, setSelectedEndpointParams] = useState<ParamItem[]>([]);
  
  // 简单类型参数的值
  const [simpleTestData, setSimpleTestData] = useState<Record<string, string>>({});

  // 简单类型参数的验证错误
  const [simpleParamErrors, setSimpleParamErrors] = useState<Record<string, string>>({});

  // 复杂类型参数的值
  const [complexTestData, setComplexTestData] = useState<Record<string, ChildParam[]>>({});

  // 复杂类型参数子参数的验证错误
  const [childParamErrors, setChildParamErrors] = useState<Record<string, Record<string, string>>>({});

  // 全局参数类型不匹配记录
  const [typeMismatchParams, setTypeMismatchParams] = useState<Set<string>>(new Set());

  // 是否显示全局参数选择器
    const [selectorVisible, setSelectorVisible] = useState(false);
  const [selectorTargetParam, setSelectorTargetParam] = useState<string | null>(null);
  const [isAddingChildParam, setIsAddingChildParam] = useState(false);

  useEffect(() => {
    if (currentDocument?.id) {
      fetchEndpoints();
    }
  }, [currentDocument]);

  const fetchEndpoints = async () => {
    if (!currentDocument?.id) return;
    try {
      const res = await endpointApi.list(currentDocument.id);
      setEndpoints(res.data);
    } catch (error) {
      message.error('加载接口列表失败');
    }
  };

  const fetchTestCases = async (endpointId: number) => {
    setLoading(true);
    try {
      const res = await testCaseApi.list(endpointId);
      setTestCases(res.data);
    } catch (error) {
      message.error('加载测试用例失败');
    } finally {
      setLoading(false);
    }
  };

  const handleSelectEndpoint = async (value: number) => {
    if (selectedEndpointId && selectedEndpointId !== value) {
      try {
        await testCaseApi.deleteByEndpoint(selectedEndpointId);
      } catch (error) {
        console.error('删除之前的测试用例失败:', error);
      }
    }
    
    setSelectedEndpointId(value);
    setTestCases([]);
    
    const selectedEp = endpoints.find(ep => ep.id === value);
    if (selectedEp && selectedEp.parameters) {
      setSelectedEndpointParams(selectedEp.parameters || []);
      
      // 初始化简单类型和复杂类型参数
      const simple: Record<string, string> = {};
      const complex: Record<string, ChildParam[]> = {};
      
      (selectedEp.parameters || []).forEach((p: ParamItem) => {
        if (p.dataType === 'object' || p.dataType === 'array') {
          complex[p.name] = [];
        } else {
          simple[p.name] = '';
        }
      });
      
      setSimpleTestData(simple);
      setComplexTestData(complex);
    } else {
      setSelectedEndpointParams([]);
      setSimpleTestData({});
      setComplexTestData({});
    }
  };

  const handleSimpleDataChange = (paramName: string, value: string, dataType: string) => {
    // 验证输入值
    const validation = validateParameterValue(value, dataType as DataType);

    setSimpleTestData(prev => ({
      ...prev,
      [paramName]: value
    }));

    // 更新验证错误
    setSimpleParamErrors(prev => ({
      ...prev,
      [paramName]: validation.valid ? '' : (validation.message || '')
    }));
  };

  const handleAddChildParam = (complexParamName: string) => {
    setSelectorTargetParam(complexParamName);
    setIsAddingChildParam(true);
    setSelectorVisible(true);
  };

  const handleDeleteChildParam = (complexParamName: string, childParamName: string) => {
    setComplexTestData(prev => ({
      ...prev,
      [complexParamName]: prev[complexParamName].filter(p => p.name !== childParamName)
    }));
  };

  const handleSelectChildParam = (param: any) => {
    if (selectorTargetParam) {
      const newChild: ChildParam = {
        name: param.name,
        value: param.exampleValue || ''
      };
      
      setComplexTestData(prev => ({
        ...prev,
        [selectorTargetParam]: [...(prev[selectorTargetParam] || []), newChild]
      }));
    }
    
    setSelectorVisible(false);
    setSelectorTargetParam(null);
    setIsAddingChildParam(false);
  };

  const handleChildParamValueChange = (complexParamName: string, childParamName: string, value: string, dataType: string) => {
    // 验证输入值
    const validation = validateParameterValue(value, dataType as DataType);

    setComplexTestData(prev => ({
      ...prev,
      [complexParamName]: prev[complexParamName].map(p =>
        p.name === childParamName ? { ...p, value } : p
      )
    }));

    // 更新验证错误
    setChildParamErrors(prev => ({
      ...prev,
      [complexParamName]: {
        ...(prev[complexParamName] || {}),
        [childParamName]: validation.valid ? '' : (validation.message || '')
      }
    }));
  };

  const buildTestData = () => {
    const result: Record<string, any> = {};
    
    // 添加简单类型参数
    Object.entries(simpleTestData).forEach(([key, value]) => {
      if (value) {
        result[key] = value;
      }
    });
    
    // 添加复杂类型参数
    Object.entries(complexTestData).forEach(([key, children]) => {
      if (children.length > 0) {
        const complexObj: Record<string, any> = {};
        children.forEach(child => {
          if (child.value) {
            complexObj[child.name] = child.value;
          }
        });
        if (Object.keys(complexObj).length > 0) {
          result[key] = complexObj;
        }
      }
    });
    
    return result;
  };

  const handleGenerate = async () => {
    if (!selectedEndpointId) {
      message.warning('Please select an endpoint first');
      return;
    }

    // 检查必填参数
    const missingRequired = selectedEndpointParams
      .filter(p => p.required && p.location === 'REQUEST_BODY')
      .filter(p => {
        if (p.dataType === 'object' || p.dataType === 'array') {
          return !complexTestData[p.name] || complexTestData[p.name].length === 0;
        } else {
          return !simpleTestData[p.name] || simpleTestData[p.name].trim() === '';
        }
      })
      .map(p => p.name);

    if (missingRequired.length > 0) {
      message.warning(`请填写必填参数: ${missingRequired.join(', ')}`);
      return;
    }

    // 验证所有参数的参数类型
    let hasTypeErrors = false;

    // 验证简单类型参数
    const simpleParams = selectedEndpointParams.filter(p =>
      p.location === 'REQUEST_BODY' && p.dataType !== 'object' && p.dataType !== 'array'
    );

    for (const param of simpleParams) {
      const value = simpleTestData[param.name] || '';
      if (value) {
        const validation = validateParameterValue(value, param.dataType as DataType);
        if (!validation.valid) {
          hasTypeErrors = true;
          setSimpleParamErrors(prev => ({
            ...prev,
            [param.name]: validation.message || '参数类型错误'
          }));
        }
      }
    }

    // 验证复杂类型参数的子参数
    const complexParams = selectedEndpointParams.filter(p =>
      p.location === 'REQUEST_BODY' && (p.dataType === 'object' || p.dataType === 'array')
    );

    for (const param of complexParams) {
      const children = complexTestData[param.name] || [];
      for (const child of children) {
        if (child.value) {
          const validation = validateParameterValue(child.value, (child.dataType || 'string') as DataType);
          if (!validation.valid) {
            hasTypeErrors = true;
            setChildParamErrors(prev => ({
              ...prev,
              [param.name]: {
                ...(prev[param.name] || {}),
                [child.name]: validation.message || '参数类型错误'
              }
            }));
          }
        }
      }
    }

    if (hasTypeErrors) {
      message.error('请输入正确参数类型再尝试生成');
      return;
    }

    // 检查是否有类型不匹配的选择
    if (typeMismatchParams.size > 0) {
      message.error(`存在 ${typeMismatchParams.size} 个参数类型不匹配，请重新选择正确的参数类型`);
      return;
    }

    setLoading(true);
    try {
      const testData = buildTestData();
      await testCaseApi.generate(selectedEndpointId, testData);
      message.success('测试用例已生成');
      fetchTestCases(selectedEndpointId);
    } catch (error) {
      message.error('生成测试用例失败');
    } finally {
      setLoading(false);
    }
  };

  const handleExportCurl = async () => {
    if (!selectedEndpointId) {
      message.warning('请先选择一个接口');
      return;
    }
    try {
      const res = await testCaseApi.exportCurl(selectedEndpointId);
      setCurlOutput(res.data);
      setCurlModalVisible(true);
    } catch (error) {
      message.error('导出cURL失败');
    }
  };

  const handleExportPostman = async () => {
    if (!selectedEndpointId) {
      message.warning('请先选择一个接口');
      return;
    }
    try {
      const res = await testCaseApi.exportPostman(selectedEndpointId);
      setPostmanOutput(JSON.stringify(res.data, null, 2));
      setPostmanModalVisible(true);
    } catch (error) {
      message.error('导出Postman集合失败');
    }
  };

  const downloadFile = (content: string, filename: string, type: string) => {
    const blob = new Blob([content], { type });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    URL.revokeObjectURL(url);
  };

  const copyToClipboard = (text: string) => {
    navigator.clipboard.writeText(text);
    message.success('Copied to clipboard');
  };

  const testCaseColumns = [
    { title: 'Name', dataIndex: 'name', key: 'name' },
    { title: 'Type', dataIndex: 'type', key: 'type', render: (type: string) => <Tag>{type}</Tag> },
    {
      title: 'Request Config',
      dataIndex: 'requestConfig',
      key: 'requestConfig',
      render: (config: any) => <Text code>{JSON.stringify(config)}</Text>,
    },
    {
      title: 'Expected Response',
      dataIndex: 'expectedResponse',
      key: 'expectedResponse',
      render: (response: any) => <Text code>{JSON.stringify(response)}</Text>,
    },
  ];

  const renderSimpleParamInput = (param: ParamItem) => {
    const hasError = simpleParamErrors[param.name];
    const errorMsg = simpleParamErrors[param.name];

    return (
    <Form.Item
      key={param.name}
      label={`${param.name}${param.required ? ' *' : ''}`}
      style={{ marginBottom: 12 }}
      validateStatus={hasError ? 'error' : ''}
      help={errorMsg}
    >
      <Space.Compact style={{ width: '100%' }}>
        <Input
          placeholder={`示例: ${param.example || param.dataType}`}
          value={simpleTestData[param.name] || ''}
          onChange={(e) => handleSimpleDataChange(param.name, e.target.value, param.dataType)}
          style={{ flex: 1, borderColor: hasError ? '#ff4d4f' : undefined }}
        />
        <Button
          icon={<DatabaseOutlined />}
          onClick={() => {
            setSelectorTargetParam(param.name);
            setIsAddingChildParam(false);
            setSelectorVisible(true);
          }}
          title="从全局参数选择"
        />
      </Space.Compact>
    </Form.Item>
  );
  };

  const renderComplexParamInput = (param: ParamItem) => {
    return (
    <Card
      key={param.name}
      size="small"
      style={{ marginBottom: 16, background: '#fafafa' }}
      title={
        <Space>
          <Text strong>{param.name}</Text>
          <Tag color="cyan">{param.dataType.toUpperCase()}</Tag>
          {param.required && <Tag color="red">必填</Tag>}
        </Space>
      }
      extra={
        <Button
          size="small"
          icon={<PlusOutlined />}
          onClick={() => handleAddChildParam(param.name)}
        >
          添加子参数
        </Button>
      }
    >
      {complexTestData[param.name]?.length > 0 ? (
        <div>
          {complexTestData[param.name].map((child) => {
            const hasError = childParamErrors[param.name]?.[child.name];
            return (
            <div key={child.name} style={{ marginBottom: 12 }}>
              <Space.Compact style={{ width: '100%' }}>
                <Input
                  addonBefore={child.name}
                  placeholder="输入值"
                  value={child.value}
                  onChange={(e) => handleChildParamValueChange(param.name, child.name, e.target.value, child.dataType || 'string')}
                  style={{ flex: 1, borderColor: hasError ? '#ff4d4f' : undefined }}
                  status={hasError ? 'error' : undefined}
                />
                <Button
                  danger
                  icon={<DeleteOutlined />}
                  onClick={() => handleDeleteChildParam(param.name, child.name)}
                />
              </Space.Compact>
              {hasError && (
                <div style={{ color: '#ff4d4f', fontSize: '12px', marginTop: 4 }}>
                  {hasError}
                </div>
              )}
            </div>
            );
          })}
          <Button
            size="small"
            type="link"
            icon={<PlusOutlined />}
            onClick={() => handleAddChildParam(param.name)}
          >
            添加更多子参数
          </Button>
        </div>
      ) : (
        <Text type="secondary">暂无子参数，点击"添加子参数"按钮开始添加</Text>
      )}
    </Card>
    );
  };

  return (
    <div>
      <Card title="测试用例生成" style={{ marginBottom: 16 }}>
        <Space style={{ width: '100%', marginBottom: 16 }}>
          <Select
            placeholder="选择一个接口"
            style={{ width: 300 }}
            value={selectedEndpointId}
            onChange={handleSelectEndpoint}
            options={endpoints.map((ep) => ({
              label: `${ep.method} ${ep.path}`,
              value: ep.id,
            }))}
          />
          <Button
            type="primary"
            icon={<ThunderboltOutlined />}
            onClick={handleGenerate}
            loading={loading}
          >
            生成测试用例
          </Button>
        </Space>

        {selectedEndpointParams.length > 0 && (
          <div>
            <Text strong style={{ display: 'block', marginBottom: 16 }}>测试参数</Text>
            
            {/* 简单类型参数 */}
            {selectedEndpointParams
              .filter(p => p.location === 'REQUEST_BODY' && p.dataType !== 'object' && p.dataType !== 'array')
              .map(renderSimpleParamInput)}
            
            {/* 复杂类型参数 */}
            {selectedEndpointParams
              .filter(p => p.location === 'REQUEST_BODY' && (p.dataType === 'object' || p.dataType === 'array'))
              .map(renderComplexParamInput)}
          </div>
        )}

        <Space style={{ marginTop: 16 }}>
          <Button icon={<DownloadOutlined />} onClick={handleExportCurl}>
            导出cURL
          </Button>
          <Button icon={<DownloadOutlined />} onClick={handleExportPostman}>
            导出Postman
          </Button>
        </Space>
      </Card>

      <Table
        title={() => '生成的测试用例'}
        dataSource={testCases}
        columns={testCaseColumns}
        rowKey="id"
        loading={loading}
      />

      <Modal
        title="cURL命令"
        open={curlModalVisible}
        onCancel={() => setCurlModalVisible(false)}
        footer={[
          <Button key="copy" onClick={() => copyToClipboard(curlOutput)}>
            复制
          </Button>,
          <Button key="download" type="primary" onClick={() => downloadFile(curlOutput, 'test_cases.sh', 'text/plain')}>
            下载
          </Button>,
        ]}
        width={700}
      >
        <pre style={{ background: '#f5f5f5', padding: 16, borderRadius: 4, maxHeight: 400, overflow: 'auto' }}>
          {curlOutput}
        </pre>
      </Modal>

      <Modal
        title="Postman集合"
        open={postmanModalVisible}
        onCancel={() => setPostmanModalVisible(false)}
        footer={[
          <Button key="copy" onClick={() => copyToClipboard(postmanOutput)}>
            复制
          </Button>,
          <Button key="download" type="primary" onClick={() => downloadFile(postmanOutput, 'postman_collection.json', 'application/json')}>
            下载
          </Button>,
        ]}
        width={700}
      >
        <pre style={{ background: '#f5f5f5', padding: 16, borderRadius: 4, maxHeight: 400, overflow: 'auto' }}>
          {postmanOutput}
        </pre>
      </Modal>

      <ParameterSelector
        visible={selectorVisible}
        onClose={() => {
          setSelectorVisible(false);
          setSelectorTargetParam(null);
          setIsAddingChildParam(false);
        }}
        onSelect={(param) => {
          if (isAddingChildParam) {
            // 复杂参数的子参数选择
            handleSelectChildParam(param);
          } else if (selectorTargetParam) {
            // 获取目标参数的数据类型
            const targetParam = selectedEndpointParams.find(p => p.name === selectorTargetParam);
            const targetDataType = targetParam?.dataType?.toLowerCase() || 'string';

            // 获取选择的参数的数据类型（全局参数可能是大写）
            const selectedDataType = param.dataType?.toLowerCase() || 'string';

            // 检查类型是否匹配
            if (selectedDataType !== targetDataType) {
              // 将类型名转换为首字母大写格式显示
              const formatType = (type: string) => type.charAt(0).toUpperCase() + type.slice(1).toLowerCase();
              message.error(`类型不匹配：目标参数是 ${formatType(targetDataType)} 类型，选择的参数是 ${formatType(selectedDataType)} 类型，已阻止填充`);

              // 记录类型不匹配
              setTypeMismatchParams(prev => {
                const newSet = new Set(prev);
                newSet.add(selectorTargetParam);
                return newSet;
              });

              setSelectorVisible(false);
              setSelectorTargetParam(null);
              return; // 阻止填充
            }

            // 类型匹配，清除之前的不匹配记录
            setTypeMismatchParams(prev => {
              const newSet = new Set(prev);
              newSet.delete(selectorTargetParam);
              return newSet;
            });

            // 设置值
            handleSimpleDataChange(selectorTargetParam, param.exampleValue || '', targetDataType);
            setSelectorVisible(false);
            setSelectorTargetParam(null);
          }
        }}
        selectSimpleTypesOnly={true}
      />
    </div>
  );
};

export default TestCaseGenerator;
