import { create } from 'zustand';

interface Project {
  id?: number;
  name: string;
  description: string;
  baseUrl: string;
  version: string;
}

interface ApiDocument {
  id?: number;
  projectId: number;
  name: string;
  description: string;
  tags: string[];
  version: string;
  deprecated: boolean;
}

interface ApiEndpoint {
  id?: number;
  documentId: number;
  path: string;
  method: string;
  summary: string;
  description: string;
  deprecated: boolean;
}

type CurrentPage = 'home' | 'endpoints' | 'testcases' | 'export' | 'globalParameters' | 'parameterTemplates';

interface AppState {
  projects: Project[];
  currentProject: Project | null;
  documents: ApiDocument[];
  currentDocument: ApiDocument | null;
  endpoints: ApiEndpoint[];
  currentEndpoint: ApiEndpoint | null;
  currentPage: CurrentPage;

  setProjects: (projects: Project[]) => void;
  setCurrentProject: (project: Project | null) => void;
  setDocuments: (documents: ApiDocument[]) => void;
  setCurrentDocument: (document: ApiDocument | null) => void;
  setEndpoints: (endpoints: ApiEndpoint[]) => void;
  setCurrentEndpoint: (endpoint: ApiEndpoint | null) => void;
  setCurrentPage: (page: CurrentPage) => void;
}

export const useAppStore = create<AppState>((set) => ({
  projects: [],
  currentProject: null,
  documents: [],
  currentDocument: null,
  endpoints: [],
  currentEndpoint: null,
  currentPage: 'home',

  setProjects: (projects) => set({ projects }),
  setCurrentProject: (currentProject) => set({ currentProject }),
  setDocuments: (documents) => set({ documents }),
  setCurrentDocument: (currentDocument) => set({ currentDocument }),
  setEndpoints: (endpoints) => set({ endpoints }),
  setCurrentEndpoint: (currentEndpoint) => set({ currentEndpoint }),
  setCurrentPage: (currentPage) => set({ currentPage }),
}));
