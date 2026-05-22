import { useState, useEffect } from 'react';
import { Sidebar } from './components/Sidebar';
import { Header } from './components/Header';
import { DashboardView } from './components/DashboardView';
import { BoardListView } from './components/BoardListView';
import { BoardDetailModal } from './components/BoardDetailModal';
import { BoardFormModal } from './components/BoardFormModal';
import { ToastContainer } from './components/ToastContainer';
import { Board, PageInfo, Toast } from './types';

function App() {
  const [activeMenu, setActiveMenu] = useState<'dashboard' | 'board' | 'settings'>('dashboard');
  const [theme, setTheme] = useState<'light' | 'dark'>('light');
  
  // 데이터 상태
  const [boards, setBoards] = useState<Board[]>([]);
  const [pageInfo, setPageInfo] = useState<PageInfo | null>(null);
  const [currentPage, setCurrentPage] = useState<number>(1);
  const [totalElement, setTotalElement] = useState<number>(0);

  // 모달 상태
  const [isDetailOpen, setIsDetailOpen] = useState(false);
  const [selectedBoard, setSelectedBoard] = useState<Board | null>(null);
  const [isDetailLoading, setIsDetailLoading] = useState(false);
  
  const [isWriteOpen, setIsWriteOpen] = useState(false);

  // 알림 토스트 상태
  const [toasts, setToasts] = useState<Toast[]>([]);

  // 테마 초기 로드 및 동기화
  useEffect(() => {
    const savedTheme = localStorage.getItem('theme') as 'light' | 'dark' | null;
    const systemPrefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
    const initialTheme = savedTheme || (systemPrefersDark ? 'dark' : 'light');
    
    setTheme(initialTheme);
    document.documentElement.setAttribute('data-theme', initialTheme);
  }, []);

  // 데이터 로드
  useEffect(() => {
    fetchBoards(currentPage);
  }, [currentPage]);

  // 테마 전환
  const toggleTheme = () => {
    const nextTheme = theme === 'light' ? 'dark' : 'light';
    setTheme(nextTheme);
    document.documentElement.setAttribute('data-theme', nextTheme);
    localStorage.setItem('theme', nextTheme);
    addToast(`${nextTheme === 'light' ? '라이트 모드' : '다크 모드'}로 변경되었습니다.`, 'info');
  };

  // 토스트 추가 유틸리티
  const addToast = (message: string, type: 'success' | 'error' | 'info') => {
    const id = Date.now();
    setToasts((prev) => [...prev, { id, message, type }]);
    
    // 3초 후 토스트 제거
    setTimeout(() => {
      setToasts((prev) => prev.filter((toast) => toast.id !== id));
    }, 3000);
  };

  // 게시판 목록 API 호출
  const fetchBoards = async (page: number) => {
    try {
      const response = await fetch(`/boards?pageNum=${page}&pageSize=10`);
      if (!response.ok) {
        throw new Error('API 호출에 실패하였습니다.');
      }
      const data = await response.json();
      
      setBoards(data.content || []);
      setPageInfo(data.pageInfo || null);
      setTotalElement(data.pageInfo?.totalElement || 0);
    } catch (err) {
      console.error(err);
      addToast('게시물 목록을 불러오는 중에 서버 에러가 발생했습니다.', 'error');
    }
  };

  // 게시판 단건 조회 API 호출
  const handleViewDetail = async (id: number) => {
    setIsDetailOpen(true);
    setIsDetailLoading(true);
    setSelectedBoard(null);
    try {
      const response = await fetch(`/boards/${id}`);
      if (!response.ok) {
        throw new Error('상세 정보를 가져오지 못했습니다.');
      }
      const data = await response.json();
      setSelectedBoard(data);
    } catch (err) {
      console.error(err);
      addToast('게시물 상세 정보를 가져오지 못했습니다.', 'error');
      setIsDetailOpen(false);
    } finally {
      setIsDetailLoading(false);
    }
  };

  // 게시글 작성 API 호출
  const handleCreateBoard = async (title: string, contents: string): Promise<boolean> => {
    try {
      const response = await fetch('/boards', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ title, contents }),
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.message || '등록 실패');
      }

      addToast('게시글이 성공적으로 등록되었습니다.', 'success');
      
      // 목록 첫 페이지로 복귀 및 다시 그리기
      if (currentPage === 1) {
        fetchBoards(1);
      } else {
        setCurrentPage(1);
      }
      return true;
    } catch (err) {
      console.error(err);
      addToast('게시글 작성 중 에러가 발생했습니다.', 'error');
      return false;
    }
  };

  const getPageTitle = () => {
    switch (activeMenu) {
      case 'dashboard': return '대시보드';
      case 'board': return '게시물 목록';
      case 'settings': return '시스템 설정';
      default: return '어드민 패널';
    }
  };

  return (
    <div className="app-container">
      {/* Sidebar Layout */}
      <Sidebar activeMenu={activeMenu} setActiveMenu={setActiveMenu} />

      {/* Main Content Layout */}
      <div className="main-content">
        <Header 
          title={getPageTitle()} 
          theme={theme} 
          toggleTheme={toggleTheme} 
        />
        
        <main className="content-body">
          {activeMenu === 'dashboard' && (
            <DashboardView 
              totalElement={totalElement}
              boards={boards}
              onViewDetail={handleViewDetail}
              onNavigateToBoard={() => setActiveMenu('board')}
            />
          )}

          {activeMenu === 'board' && (
            <BoardListView 
              boards={boards}
              pageInfo={pageInfo}
              onPageChange={setCurrentPage}
              onOpenWriteModal={() => setIsWriteOpen(true)}
              onViewDetail={handleViewDetail}
            />
          )}

          {activeMenu === 'settings' && (
            <div className="section-card" style={{ maxWidth: '800px' }}>
              <div className="section-header">
                <h2 className="section-title">시스템 설정 및 정보</h2>
              </div>
              <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', marginBottom: '24px' }}>
                게시판 관리 도구의 시스템 환경 설정입니다. 현재 세션에 활성화된 세부 환경 정보를 제공합니다.
              </p>

              <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                <div style={{ padding: '16px', borderRadius: '8px', border: '1px solid var(--border-color)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <div>
                    <strong style={{ display: 'block', fontSize: '0.95rem' }}>다크 모드 강제 적용</strong>
                    <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>브라우저 환경에 관계없이 다크 모드 활성화</span>
                  </div>
                  <button className="btn btn-secondary" onClick={toggleTheme}>
                    {theme === 'dark' ? '다크 모드 활성' : '라이트 모드 활성'}
                  </button>
                </div>

                <div style={{ padding: '16px', borderRadius: '8px', border: '1px solid var(--border-color)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <div>
                    <strong style={{ display: 'block', fontSize: '0.95rem' }}>백엔드 API 서버 상태</strong>
                    <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>통신 엔드포인트: localhost:8080</span>
                  </div>
                  <span style={{ 
                    padding: '4px 8px', 
                    borderRadius: '4px', 
                    fontSize: '0.75rem', 
                    fontWeight: 700, 
                    backgroundColor: 'rgba(16, 185, 129, 0.1)', 
                    color: 'var(--success)'
                  }}>
                    연결됨
                  </span>
                </div>

                <div style={{ padding: '16px', borderRadius: '8px', border: '1px solid var(--border-color)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <div>
                    <strong style={{ display: 'block', fontSize: '0.95rem' }}>어플리케이션 버전 정보</strong>
                    <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>정적 파일 및 번들 버전</span>
                  </div>
                  <span style={{ fontSize: '0.9rem', fontWeight: 600 }}>v1.0.0-snapshot</span>
                </div>
              </div>
            </div>
          )}
        </main>
      </div>

      {/* Modals & Overlay Portals */}
      <BoardDetailModal 
        isOpen={isDetailOpen}
        board={selectedBoard}
        isLoading={isDetailLoading}
        onClose={() => setIsDetailOpen(false)}
      />

      <BoardFormModal 
        isOpen={isWriteOpen}
        onSubmit={handleCreateBoard}
        onClose={() => setIsWriteOpen(false)}
      />

      {/* Dynamic Toast Container */}
      <ToastContainer toasts={toasts} />
    </div>
  );
}

export default App;
