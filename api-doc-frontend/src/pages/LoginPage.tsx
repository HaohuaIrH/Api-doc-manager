import React, { useState } from 'react';
import { Form, Input, Button, Card, Tabs, message } from 'antd';
import { UserOutlined, LockOutlined, MailOutlined, PhoneOutlined } from '@ant-design/icons';
import { authApi } from '../services/api';
import { useAuthStore } from '../stores/authStore';


interface LoginForm {
  username: string;
  password: string;
}

interface RegisterForm {
  username: string;
  password: string;
  confirmPassword: string;
  email?: string;
  fullName?: string;
}

const LoginPage: React.FC = () => {
  const [loginLoading, setLoginLoading] = useState(false);
  const [registerLoading, setRegisterLoading] = useState(false);
  const [activeTab, setActiveTab] = useState('login');
  const { setAuth } = useAuthStore();
  const [messageApi, contextHolder] = message.useMessage();

  const onLogin = async (values: LoginForm) => {
    setLoginLoading(true);
    try {
      const response = await authApi.login(values.username, values.password);
      // 后端返回的是 accessToken，前端读取正确字段名
      const { accessToken: token, username } = response.data;
      setAuth(
        { username: username || values.username },
        token
      );
      messageApi.success('登录成功！');
    } catch (error: any) {
      const errorMsg = error.response?.data?.message || '登录失败，请检查用户名和密码';
      messageApi.error(errorMsg);
    } finally {
      setLoginLoading(false);
    }
  };

  const onRegister = async (values: RegisterForm) => {
    if (values.password !== values.confirmPassword) {
      messageApi.error('两次输入的密码不一致');
      return;
    }
    if (values.password.length < 6) {
      messageApi.error('密码长度至少为6位');
      return;
    }

    setRegisterLoading(true);
    try {
      // 注册成功后自动登录
      const response = await authApi.register({
        username: values.username,
        password: values.password,
        email: values.email,
        fullName: values.fullName,
      });

      // 自动登录 - 后端返回的是 accessToken
      const { accessToken: token, username } = response.data;
      setAuth(
        { username: username || values.username },
        token
      );
      messageApi.success('注册并登录成功！');
    } catch (error: any) {
      const errorMsg = error.response?.data?.message || '注册失败，用户名可能已被占用';
      messageApi.error(errorMsg);
    } finally {
      setRegisterLoading(false);
    }
  };

  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
        padding: '20px',
      }}
    >
      {contextHolder}
      <Card
        style={{
          width: 420,
          boxShadow: '0 10px 40px rgba(0,0,0,0.2)',
          borderRadius: 12,
        }}
        styles={{ body: { padding: 0 } }}
      >
        <div
          style={{
            background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
            padding: '30px 24px',
            textAlign: 'center',
            borderRadius: '12px 12px 0 0',
          }}
        >
          <h1 style={{ color: 'white', margin: 0, fontSize: 28, fontWeight: 'bold' }}>
            API Doc Manager
          </h1>
          <p style={{ color: 'rgba(255,255,255,0.8)', margin: '8px 0 0 0', fontSize: 14 }}>
            专业的API接口文档管理工具
          </p>
        </div>

        <div style={{ padding: '24px 24px 32px 24px' }}>
          <Tabs
            activeKey={activeTab}
            onChange={setActiveTab}
            centered
            style={{ marginBottom: 16 }}
            items={[
              {
                key: 'login',
                label: '登录',
                children: (
                  <Form
                    name="login"
                    onFinish={onLogin}
                    layout="vertical"
                    requiredMark={false}
                    size="large"
                  >
                    <Form.Item
                      name="username"
                      rules={[{ required: true, message: '请输入用户名' }]}
                    >
                      <Input
                        prefix={<UserOutlined style={{ color: '#bfbfbf' }} />}
                        placeholder="用户名"
                      />
                    </Form.Item>

                    <Form.Item
                      name="password"
                      rules={[{ required: true, message: '请输入密码' }]}
                    >
                      <Input.Password
                        prefix={<LockOutlined style={{ color: '#bfbfbf' }} />}
                        placeholder="密码"
                      />
                    </Form.Item>

                    <Form.Item>
                      <Button
                        type="primary"
                        htmlType="submit"
                        loading={loginLoading}
                        block
                        style={{
                          height: 44,
                          borderRadius: 6,
                          fontSize: 16,
                          fontWeight: 500,
                        }}
                      >
                        登录
                      </Button>
                    </Form.Item>
                  </Form>
                ),
              },
              {
                key: 'register',
                label: '注册',
                children: (
                  <Form
                    name="register"
                    onFinish={onRegister}
                    layout="vertical"
                    requiredMark={false}
                    size="large"
                  >
                    <Form.Item
                      name="username"
                      rules={[
                        { required: true, message: '请输入用户名' },
                        { min: 3, message: '用户名至少3个字符' },
                      ]}
                    >
                      <Input
                        prefix={<UserOutlined style={{ color: '#bfbfbf' }} />}
                        placeholder="用户名 (必填)"
                      />
                    </Form.Item>

                    <Form.Item
                      name="password"
                      rules={[
                        { required: true, message: '请输入密码' },
                        { min: 6, message: '密码至少6个字符' },
                      ]}
                    >
                      <Input.Password
                        prefix={<LockOutlined style={{ color: '#bfbfbf' }} />}
                        placeholder="密码 (至少6位)"
                      />
                    </Form.Item>

                    <Form.Item
                      name="confirmPassword"
                      rules={[
                        { required: true, message: '请确认密码' },
                        ({ getFieldValue }) => ({
                          validator(_, value) {
                            if (!value || getFieldValue('password') === value) {
                              return Promise.resolve();
                            }
                            return Promise.reject(new Error('两次输入的密码不一致'));
                          },
                        }),
                      ]}
                    >
                      <Input.Password
                        prefix={<LockOutlined style={{ color: '#bfbfbf' }} />}
                        placeholder="确认密码"
                      />
                    </Form.Item>

                    <Form.Item name="email">
                      <Input
                        prefix={<MailOutlined style={{ color: '#bfbfbf' }} />}
                        placeholder="邮箱 (选填)"
                        type="email"
                      />
                    </Form.Item>

                    <Form.Item name="fullName">
                      <Input
                        prefix={<PhoneOutlined style={{ color: '#bfbfbf' }} />}
                        placeholder="姓名 (选填)"
                      />
                    </Form.Item>

                    <Form.Item>
                      <Button
                        type="primary"
                        htmlType="submit"
                        loading={registerLoading}
                        block
                        style={{
                          height: 44,
                          borderRadius: 6,
                          fontSize: 16,
                          fontWeight: 500,
                        }}
                      >
                        注册
                      </Button>
                    </Form.Item>
                  </Form>
                ),
              },
            ]}
          />
        </div>
      </Card>
    </div>
  );
};

export default LoginPage;
