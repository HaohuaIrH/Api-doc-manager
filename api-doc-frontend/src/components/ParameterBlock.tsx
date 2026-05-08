import React from 'react';
import { Tag, Tooltip } from 'antd';
import { DeleteOutlined } from '@ant-design/icons';

interface ParameterBlockProps {
  parameter: {
    id?: number;
    name: string;
    dataType: string;
    exampleValue?: string;
    description?: string;
  };
  onClick?: (parameter: any) => void;
  onDelete?: (id: number) => void;
  showDelete?: boolean;
  style?: React.CSSProperties;
}

const ParameterBlock: React.FC<ParameterBlockProps> = ({
  parameter,
  onClick,
  onDelete,
  showDelete = false,
  style
}) => {
  const getTypeColor = (type: string) => {
    switch (type.toUpperCase()) {
      case 'STRING':
        return 'blue';
      case 'INTEGER':
      case 'LONG':
      case 'DOUBLE':
        return 'green';
      case 'BOOLEAN':
        return 'orange';
      case 'ARRAY':
        return 'purple';
      case 'OBJECT':
        return 'cyan';
      default:
        return 'default';
    }
  };

  const content = (
    <div
      onClick={() => onClick?.(parameter)}
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: 8,
        padding: '8px 12px',
        background: '#f0f5ff',
        border: '1px solid #d9d9d9',
        borderRadius: 6,
        cursor: onClick ? 'pointer' : 'default',
        transition: 'all 0.3s',
        minWidth: 100,
        maxWidth: 250,
        ...style
      }}
      onMouseEnter={(e) => {
        if (onClick) {
          e.currentTarget.style.borderColor = '#1890ff';
          e.currentTarget.style.background = '#e6f7ff';
        }
      }}
      onMouseLeave={(e) => {
        if (onClick) {
          e.currentTarget.style.borderColor = '#d9d9d9';
          e.currentTarget.style.background = '#f0f5ff';
        }
      }}
    >
      <span style={{ fontWeight: 500, fontSize: 14, flex: 1 }}>
        {parameter.name}
      </span>
      <Tag color={getTypeColor(parameter.dataType)} style={{ marginRight: 0 }}>
        {parameter.dataType.toUpperCase()}
      </Tag>
      {showDelete && parameter.id && onDelete && (
        <DeleteOutlined
          onClick={(e) => {
            e.stopPropagation();
            onDelete(parameter.id!);
          }}
          style={{ color: '#ff4d4f', cursor: 'pointer', fontSize: 14 }}
        />
      )}
    </div>
  );

  if (parameter.exampleValue || parameter.description) {
    return (
      <Tooltip
        title={
          <div>
            {parameter.exampleValue && (
              <div><strong>示例值:</strong> {parameter.exampleValue}</div>
            )}
            {parameter.description && (
              <div><strong>描述:</strong> {parameter.description}</div>
            )}
          </div>
        }
      >
        {content}
      </Tooltip>
    );
  }

  return content;
};

export default ParameterBlock;
