import axios from 'axios';

const API_BASE_URL = 'http://localhost:8081/api';

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

// Auth APIs
export const authApi = {
  login: (username: string, password: string) =>
    apiClient.post('/auth/login', { username, password }),
  register: (data: { username: string; password: string; email?: string; fullName?: string }) =>
    apiClient.post('/auth/register', data),
};

// Project APIs
export const projectApi = {
  list: () => apiClient.get('/projects'),
  get: (id: number) => apiClient.get(`/projects/${id}`),
  create: (data: any) => apiClient.post('/projects', data),
  update: (id: number, data: any) => apiClient.put(`/projects/${id}`, data),
  delete: (id: number) => apiClient.delete(`/projects/${id}`),
  deletePermanently: (id: number) => apiClient.delete(`/projects/${id}/permanent`),
  batchDelete: (ids: number[]) => apiClient.post('/projects/batch-delete', ids),
  batchDeletePermanently: (ids: number[]) => apiClient.delete('/projects/batch-delete/permanent', { data: ids }),
};

// Document APIs
export const documentApi = {
  list: (projectId: number) => apiClient.get(`/documents/project/${projectId}`),
  get: (id: number) => apiClient.get(`/documents/${id}`),
  create: (data: any) => apiClient.post('/documents', data),
  update: (id: number, data: any) => apiClient.put(`/documents/${id}`, data),
  delete: (id: number) => apiClient.delete(`/documents/${id}`),
  deletePermanently: (id: number) => apiClient.delete(`/documents/${id}/permanent`),
  batchDelete: (ids: number[]) => apiClient.post('/documents/batch-delete', ids),
  batchDeletePermanently: (ids: number[]) => apiClient.delete('/documents/batch-delete/permanent', { data: ids }),
};

// Endpoint APIs
export const endpointApi = {
  list: (documentId: number) => apiClient.get(`/endpoints/document/${documentId}`),
  get: (id: number) => apiClient.get(`/endpoints/${id}`),
  create: (data: any) => apiClient.post('/endpoints', data),
  update: (id: number, data: any) => apiClient.put(`/endpoints/${id}`, data),
  delete: (id: number) => apiClient.delete(`/endpoints/${id}`),
  // 即时测试接口，直接调用API不生成TestCase
  test: (id: number, params?: Record<string, any>) => apiClient.post(`/endpoints/${id}/test`, params),
};

// Parameter APIs
export const parameterApi = {
  list: (endpointId: number) => apiClient.get(`/parameters/endpoint/${endpointId}`),
  create: (data: any) => apiClient.post('/parameters', data),
  update: (id: number, data: any) => apiClient.put(`/parameters/${id}`, data),
  delete: (id: number) => apiClient.delete(`/parameters/${id}`),
};

// Test Case APIs
export const testCaseApi = {
  list: (endpointId: number) => apiClient.get(`/testcases/endpoint/${endpointId}`),
  generate: (endpointId: number, testData?: Record<string, any>) => 
    apiClient.post(`/testcases/generate/${endpointId}`, { testData }),
  exportCurl: (endpointId: number) => apiClient.get(`/testcases/export/curl/${endpointId}`),
  exportPostman: (endpointId: number) => apiClient.get(`/testcases/export/postman/${endpointId}`),
  deleteByEndpoint: (endpointId: number) => apiClient.delete(`/testcases/endpoint/${endpointId}`),
};

// Export APIs
export const exportApi = {
  exportMarkdown: (documentId: number) =>
    apiClient.get(`/export/markdown/${documentId}`, { responseType: 'text' }),
  exportLatex: (documentId: number) =>
    apiClient.get(`/export/latex/${documentId}`, { responseType: 'text' }),
};

// Global Parameter APIs
export const globalParameterApi = {
  list: () => apiClient.get('/global-parameters'),
  get: (id: number) => apiClient.get(`/global-parameters/${id}`),
  create: (data: any) => apiClient.post('/global-parameters', data),
  update: (id: number, data: any) => apiClient.put(`/global-parameters/${id}`, data),
  delete: (id: number) => apiClient.delete(`/global-parameters/${id}`),
  getSimpleTypes: () => apiClient.get('/global-parameters/simple-types'),
  getComplexTypes: () => apiClient.get('/global-parameters/complex-types'),
  search: (keyword: string) => apiClient.get(`/global-parameters/search?q=${encodeURIComponent(keyword)}`),
};

// Parameter Template APIs
export const parameterTemplateApi = {
  list: (documentId: number) => apiClient.get(`/parameter-templates?documentId=${documentId}`),
  getFolders: (documentId: number) => apiClient.get(`/parameter-templates/folders?documentId=${documentId}`),
  getByFolder: (folderName: string, documentId: number) => 
    apiClient.get(`/parameter-templates/folder/${encodeURIComponent(folderName)}?documentId=${documentId}`),
  create: (data: any) => apiClient.post('/parameter-templates', data),
  createFromEndpoint: (endpointId: number) => 
    apiClient.post(`/parameter-templates/from-endpoint/${endpointId}`),
  delete: (id: number) => apiClient.delete(`/parameter-templates/${id}`),
  deleteFolder: (folderName: string, documentId: number) => 
    apiClient.delete(`/parameter-templates/folder/${encodeURIComponent(folderName)}?documentId=${documentId}`),
};

export default apiClient;
