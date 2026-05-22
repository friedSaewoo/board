export interface Board {
  boardId: number;
  title: string;
  contents: string;
  createdAt: string;
  updatedAt: string;
}

export interface PageInfo {
  pageNum: number;
  pageSize: number;
  totalElement: number;
  totalPage: number;
  blockStart: number;
  blockEnd: number;
  hasPrev: boolean;
  hasNext: boolean;
}

export interface PagedResult {
  content: Board[];
  pageInfo: PageInfo;
}

export interface Toast {
  id: number;
  message: string;
  type: 'success' | 'error' | 'info';
}
