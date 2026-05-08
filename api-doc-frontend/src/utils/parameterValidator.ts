import { Rule } from 'antd/es/form';

export type DataType = 'string' | 'integer' | 'number' | 'boolean' | 'array' | 'object';

export interface ValidationResult {
  valid: boolean;
  message?: string;
}

export function validateInteger(value: string): ValidationResult {
  if (!value || value.trim() === '') {
    return { valid: true };
  }

  const trimmed = value.trim();

  const integerRegex = /^-?\d+$/;
  if (!integerRegex.test(trimmed)) {
    return { valid: false, message: '示例值必须是整数类型' };
  }

  return { valid: true };
}

export function validateNumber(value: string): ValidationResult {
  if (!value || value.trim() === '') {
    return { valid: true };
  }

  const trimmed = value.trim();

  const numberRegex = /^-?\d+(\.\d+)?$/;
  if (!numberRegex.test(trimmed)) {
    return { valid: false, message: '示例值必须是数字类型' };
  }

  return { valid: true };
}

export function validateBoolean(value: string): ValidationResult {
  if (!value || value.trim() === '') {
    return { valid: true };
  }

  const trimmed = value.trim().toLowerCase();
  if (trimmed !== 'true' && trimmed !== 'false') {
    return { valid: false, message: '布尔类型只能是 true 或 false' };
  }

  return { valid: true };
}

export function validateArray(value: string): ValidationResult {
  if (!value || value.trim() === '') {
    return { valid: true };
  }

  const trimmed = value.trim();

  try {
    const parsed = JSON.parse(trimmed);
    if (!Array.isArray(parsed)) {
      return { valid: false, message: '示例值必须是有效的 JSON 数组格式' };
    }
    return { valid: true };
  } catch (e) {
    return { valid: false, message: '示例值必须是有效的 JSON 数组格式' };
  }
}

export function validateObject(value: string): ValidationResult {
  if (!value || value.trim() === '') {
    return { valid: true };
  }

  const trimmed = value.trim();

  try {
    const parsed = JSON.parse(trimmed);
    if (typeof parsed !== 'object' || parsed === null || Array.isArray(parsed)) {
      return { valid: false, message: '示例值必须是有效的 JSON 对象格式' };
    }
    return { valid: true };
  } catch (e) {
    return { valid: false, message: '示例值必须是有效的 JSON 对象格式' };
  }
}

export function validateParameterValue(value: string, dataType: DataType): ValidationResult {
  switch (dataType) {
    case 'string':
      return { valid: true };
    case 'integer':
      return validateInteger(value);
    case 'number':
      return validateNumber(value);
    case 'boolean':
      return validateBoolean(value);
    case 'array':
      return validateArray(value);
    case 'object':
      return validateObject(value);
    default:
      return { valid: true };
  }
}

export function validateRange(
  value: number,
  minimum?: number,
  maximum?: number
): ValidationResult {
  if (minimum !== undefined && value < minimum) {
    return { valid: false, message: `值不能小于最小值 ${minimum}` };
  }

  if (maximum !== undefined && value > maximum) {
    return { valid: false, message: `值不能大于最大值 ${maximum}` };
  }

  return { valid: true };
}

export function validateLength(
  value: string,
  minLength?: number,
  maxLength?: number
): ValidationResult {
  if (minLength !== undefined && value.length < minLength) {
    return { valid: false, message: `长度不能小于最小值 ${minLength}` };
  }

  if (maxLength !== undefined && value.length > maxLength) {
    return { valid: false, message: `长度不能大于最大值 ${maxLength}` };
  }

  return { valid: true };
}

export function createDataTypeValidator(dataType: DataType): Rule[] {
  return [
    {
      validator: (_: any, value: string) => {
        const result = validateParameterValue(value, dataType);
        if (result.valid) {
          return Promise.resolve();
        }
        return Promise.reject(result.message);
      },
    },
  ];
}

export function createRangeValidator(minimum?: number, maximum?: number): Rule[] {
  if (minimum === undefined && maximum === undefined) {
    return [];
  }

  return [
    {
      validator: (_: any, value: number) => {
        if (value === undefined || value === null || value === '') {
          return Promise.resolve();
        }

        const numValue = typeof value === 'number' ? value : parseFloat(value);

        if (isNaN(numValue)) {
          return Promise.reject('请输入有效的数字');
        }

        const result = validateRange(numValue, minimum, maximum);
        if (result.valid) {
          return Promise.resolve();
        }
        return Promise.reject(result.message);
      },
    },
  ];
}

export function createLengthValidator(minLength?: number, maxLength?: number): Rule[] {
  if (minLength === undefined && maxLength === undefined) {
    return [];
  }

  return [
    {
      validator: (_: any, value: string) => {
        if (!value || value === '') {
          return Promise.resolve();
        }

        const result = validateLength(value, minLength, maxLength);
        if (result.valid) {
          return Promise.resolve();
        }
        return Promise.reject(result.message);
      },
    },
  ];
}
