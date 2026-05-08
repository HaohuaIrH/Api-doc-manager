import React, { useState, useEffect } from 'react';
import { Modal, Input, Tabs, List, Empty, Spin } from 'antd';
import { SearchOutlined } from '@ant-design/icons';
import ParameterBlock from './ParameterBlock';
import { globalParameterApi } from '../services/api';

const { Search } = Input;

interface ParameterSelectorProps {
  visible: boolean;
  onClose: () => void;
  onSelect: (parameter: any) => void;
  selectSimpleTypesOnly?: boolean;
}

const ParameterSelector: React.FC<ParameterSelectorProps> = ({
  visible,
  onClose,
  onSelect,
  selectSimpleTypesOnly = false
}) => {
  const [loading, setLoading] = useState(false);
  const [searchText, setSearchText] = useState('');
  const [simpleTypes, setSimpleTypes] = useState<any[]>([]);
  const [complexTypes, setComplexTypes] = useState<any[]>([]);
  const [activeTab, setActiveTab] = useState('simple');

  useEffect(() => {
    if (visible) {
      loadParameters();
    }
  }, [visible]);

  const loadParameters = async () => {
    setLoading(true);
    try {
      const [simpleRes, complexRes] = await Promise.all([
        globalParameterApi.getSimpleTypes(),
        globalParameterApi.getComplexTypes()
      ]);
      setSimpleTypes(simpleRes.data || []);
      setComplexTypes(complexRes.data || []);
    } catch (error) {
      console.error('加载参数失败:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = async (value: string) => {
    if (!value.trim()) {
      loadParameters();
      return;
    }

    setLoading(true);
    try {
      const res = await globalParameterApi.search(value);
      const results = res.data || [];
      setSimpleTypes(results.filter((p: any) => !p.isComplexType));
      setComplexTypes(results.filter((p: any) => p.isComplexType));
    } catch (error) {
      console.error('搜索失败:', error);
    } finally {
      setLoading(false);
    }
  };

  const renderParameterList = (parameters: any[], isComplex = false) => {
    if (parameters.length === 0) {
      return <Empty description="暂无参数" style={{ margin: '40px 0' }} />;
    }

    return (
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 12, padding: 16 }}>
        {parameters.map((param) => (
          <ParameterBlock
            key={param.id}
            parameter={param}
            onClick={() => {
              onSelect(param);
              onClose();
            }}
            style={{ width: isComplex ? '100%' : undefined }}
          />
        ))}
      </div>
    );
  };

  const renderChildren = (children: any[]) => {
    if (!children || children.length === 0) return null;

    return (
      <div style={{ marginLeft: 20, marginTop: 8, display: 'flex', flexWrap: 'wrap', gap: 8 }}>
        {children.map((child) => (
          <ParameterBlock
            key={child.id}
            parameter={child}
            onClick={() => {
              onSelect(child);
              onClose();
            }}
          />
        ))}
      </div>
    );
  };

  return (
    <Modal
      title="选择全局参数"
      open={visible}
      onCancel={onClose}
      footer={null}
      width={700}
      destroyOnClose
    >
      <Search
        placeholder="搜索参数名称..."
        prefix={<SearchOutlined />}
        onSearch={handleSearch}
        onChange={(e) => setSearchText(e.target.value)}
        style={{ marginBottom: 16 }}
        allowClear
      />

      <Spin spinning={loading}>
        {selectSimpleTypesOnly ? (
          <div>
            {simpleTypes.length === 0 && !loading ? (
              <Empty description="暂无简单类型参数，请先在全局参数表中添加" />
            ) : (
              renderParameterList(simpleTypes)
            )}
          </div>
        ) : (
          <Tabs
            activeKey={activeTab}
            onChange={setActiveTab}
            items={[
              {
                key: 'simple',
                label: `简单类型 (${simpleTypes.length})`,
                children: (
                  <div>
                    {simpleTypes.length === 0 && !loading ? (
                      <Empty description="暂无简单类型参数" />
                    ) : (
                      renderParameterList(simpleTypes)
                    )}
                  </div>
                )
              },
              {
                key: 'complex',
                label: `复杂类型 (${complexTypes.length})`,
                children: (
                  <div>
                    {complexTypes.length === 0 && !loading ? (
                      <Empty description="暂无复杂类型参数" />
                    ) : (
                      <List
                        dataSource={complexTypes}
                        renderItem={(item) => (
                          <List.Item>
                            <div style={{ width: '100%' }}>
                              <ParameterBlock
                                parameter={item}
                                onClick={() => {
                                  onSelect(item);
                                  onClose();
                                }}
                                style={{ width: '100%', maxWidth: 'none' }}
                              />
                              {renderChildren(item.children)}
                            </div>
                          </List.Item>
                        )}
                      />
                    )}
                  </div>
                )
              }
            ]}
          />
        )}
      </Spin>
    </Modal>
  );
};

export default ParameterSelector;
