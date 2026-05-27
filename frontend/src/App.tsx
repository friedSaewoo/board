import { useState, useEffect } from 'react';
import { Sidebar } from './components/Sidebar';
import { Header } from './components/Header';
import { DashboardView } from './components/DashboardView';
import { BoardListView } from './components/BoardListView';
import { BoardDetailView } from './components/BoardDetailView';
import { BoardWriteView } from './components/BoardWriteView';
import { BoardEditView } from './components/BoardEditView';
import { ToastContainer } from './components/ToastContainer';
import { AuthView } from './components/AuthView';
import { ChessAnalysisView } from './components/ChessAnalysisView';
import { ActiveMenu, Board, Member, PageInfo, Toast } from './types';

function App() {
  const [activeMenu, setActiveMenu] = useState<ActiveMenu>('auth');
  const [theme, setTheme] = useState<'light' | 'dark'>('light');

  // 인증 상태
  const [currentUser, setCurrentUser] = useState<Member | null>(null);
  const [isAuthChecking, setIsAuthChecking] = useState(true);

  // 데이터 상태
  const [boards, setBoards] = useState<Board[]>([]);
  const [pageInfo, setPageInfo] = useState<PageInfo | null>(null);
  const [currentPage, setCurrentPage] = useState<number>(1);
  const [totalElement, setTotalElement] = useState<number>(0);

  // 가상 라우팅 상태 ('list' | 'detail' | 'write' | 'edit')
  const [boardView, setBoardView] = useState<'list' | 'detail' | 'write' | 'edit'>('list');
  const [selectedBoard, setSelectedBoard] = useState<Board | null>(null);
  const [isDetailLoading, setIsDetailLoading] = useState(false);

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

  // 세션 로그인 상태 확인
  useEffect(() => {
    checkCurrentUser();
  }, []);

  // 데이터 로드: 로그인된 경우에만 게시글 API 호출
  useEffect(() => {
    if (currentUser) {
      fetchBoards(currentPage);
      return;
    }

    setBoards([]);
    setPageInfo(null);
    setTotalElement(0);
  }, [currentPage, currentUser]);

  const checkCurrentUser = async (): Promise<Member | null> => {
    try {
      const response = await fetch('/members/me', {
        credentials: 'include',
      });

      const contentType = response.headers.get('content-type') || '';
      if (!response.ok || !contentType.includes('application/json')) {
        setCurrentUser(null);
        setActiveMenu('auth');
        return null;
      }

      const user = await response.json();
      setCurrentUser(user);
      setActiveMenu((prev) => (prev === 'auth' ? 'dashboard' : prev));
      return user;
    } catch (err) {
      console.error(err);
      setCurrentUser(null);
      setActiveMenu('auth');
      return null;
    } finally {
      setIsAuthChecking(false);
    }
  };

  // 사이드바 등 메뉴 변경 시 라우팅 초기화
  const handleMenuChange = (menu: ActiveMenu) => {
    setActiveMenu(menu);
    if (menu === 'board') {
      setBoardView('list');
    }
  };

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

  const handleLogin = async (email: string, password: string): Promise<boolean> => {
    try {
      const body = new URLSearchParams();
      body.set('username', email);
      body.set('password', password);

      await fetch('/login', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded',
        },
        body,
        credentials: 'include',
      });

      const user = await checkCurrentUser();
      if (!user) {
        addToast('이메일 또는 비밀번호를 확인해 주세요.', 'error');
        return false;
      }

      addToast(`${user.name}님, 로그인되었습니다.`, 'success');
      setActiveMenu('dashboard');
      return true;
    } catch (err) {
      console.error(err);
      addToast('로그인 요청 중 오류가 발생했습니다.', 'error');
      return false;
    }
  };

  const handleSignup = async (name: string, email: string, password: string): Promise<boolean> => {
    try {
      const response = await fetch('/members/signup', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ name, email, password }),
        credentials: 'include',
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.message || '회원가입 실패');
      }

      addToast('회원가입이 완료되었습니다. 자동 로그인합니다.', 'success');
      return handleLogin(email, password);
    } catch (err) {
      console.error(err);
      addToast(err instanceof Error ? err.message : '회원가입 요청 중 오류가 발생했습니다.', 'error');
      return false;
    }
  };

  const handleSessionExpired = () => {
    setCurrentUser(null);
    setSelectedBoard(null);
    setBoardView('list');
    setActiveMenu('auth');
    addToast('세션이 만료되었습니다. 다시 로그인해 주세요.', 'info');
  };

  const handleLogout = async () => {
    try {
      await fetch('/logout', {
        method: 'POST',
        credentials: 'include',
      });
    } catch (err) {
      console.error(err);
    } finally {
      setCurrentUser(null);
      setBoards([]);
      setPageInfo(null);
      setTotalElement(0);
      setSelectedBoard(null);
      setBoardView('list');
      setActiveMenu('auth');
      addToast('로그아웃되었습니다.', 'info');
    }
  };

  // 게시판 목록 API 호출
  const fetchBoards = async (page: number) => {
    if (!currentUser) return;

    try {
      const response = await fetch(`/boards?pageNum=${page}&pageSize=10&sortBy=id&direction=DESC`, {
        credentials: 'include',
      });
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

  // 게시판 단건 조회 API 호출 (상세 페이지로 이동)
  const handleViewDetail = async (id: number) => {
    if (!currentUser) {
      setActiveMenu('auth');
      addToast('로그인 후 게시글을 조회할 수 있습니다.', 'info');
      return;
    }

    setActiveMenu('board');
    setBoardView('detail');
    setIsDetailLoading(true);
    setSelectedBoard(null);
    try {
      const response = await fetch(`/boards/${id}`, {
        credentials: 'include',
      });
      if (!response.ok) {
        throw new Error('상세 정보를 가져오지 못했습니다.');
      }
      const data = await response.json();
      setSelectedBoard(data);
    } catch (err) {
      console.error(err);
      addToast('게시물 상세 정보를 가져오지 못했습니다.', 'error');
      setBoardView('list');
    } finally {
      setIsDetailLoading(false);
    }
  };

  // 게시글 작성 API 호출
  const handleCreateBoard = async (title: string, contents: string): Promise<boolean> => {
    if (!currentUser) {
      setActiveMenu('auth');
      addToast('로그인 후 게시글을 작성할 수 있습니다.', 'info');
      return false;
    }

    try {
      const response = await fetch('/boards', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ title, contents }),
        credentials: 'include',
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

  // 게시글 수정 API 호출
  const handleUpdateBoard = async (title: string, contents: string): Promise<boolean> => {
    if (!selectedBoard || !currentUser) return false;
    try {
      const response = await fetch(`/boards/${selectedBoard.boardId}`, {
        method: 'PATCH',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ title, contents }),
        credentials: 'include',
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.message || '수정 실패');
      }

      addToast('게시글이 성공적으로 수정되었습니다.', 'success');

      // 단건 정보 업데이트
      const updatedData = await response.json();
      setSelectedBoard(updatedData);

      // 목록 정보 갱신
      fetchBoards(currentPage);
      return true;
    } catch (err) {
      console.error(err);
      addToast('게시글 수정 중 에러가 발생했습니다.', 'error');
      return false;
    }
  };

  // 게시글 삭제 API 호출
  const handleDeleteBoard = async (id: number) => {
    if (!currentUser) {
      setActiveMenu('auth');
      addToast('로그인 후 게시글을 삭제할 수 있습니다.', 'info');
      return;
    }

    try {
      const response = await fetch(`/boards/${id}`, {
        method: 'DELETE',
        credentials: 'include',
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.message || '삭제 실패');
      }

      addToast('게시글이 성공적으로 삭제되었습니다.', 'success');

      // 목록 페이지로 이동
      setBoardView('list');
      // 목록 정보 갱신
      fetchBoards(currentPage);
    } catch (err) {
      console.error(err);
      addToast('게시글 삭제 중 에러가 발생했습니다.', 'error');
    }
  };

  const getPageTitle = () => {
    switch (activeMenu) {
      case 'auth': return currentUser ? '내 계정' : '로그인';
      case 'dashboard': return '대시보드';
      case 'board': {
        switch (boardView) {
          case 'list': return '게시물 목록';
          case 'detail': return '게시글 상세 정보';
          case 'write': return '새 게시글 등록';
          case 'edit': return '게시글 수정';
        }
      }
      case 'chess': return '체스 분석';
      case 'settings': return '시스템 설정';
      default: return '어드민 패널';
    }
  };

  const renderProtectedNotice = (message: string) => (
    <div className="section-card auth-required-card">
      <div className="metric-icon">🔐</div>
      <h2>로그인이 필요합니다</h2>
      <p>{message}</p>
      <button className="btn btn-primary" onClick={() => setActiveMenu('auth')}>
        로그인 화면으로 이동
      </button>
    </div>
  );

  return (
    <div className="app-container">
      {/* Sidebar Layout */}
      <Sidebar
        activeMenu={activeMenu}
        setActiveMenu={handleMenuChange}
        isAuthenticated={Boolean(currentUser)}
      />

      {/* Main Content Layout */}
      <div className="main-content">
        <Header
          title={getPageTitle()}
          theme={theme}
          toggleTheme={toggleTheme}
          currentUser={currentUser}
          onLogout={handleLogout}
          onNavigateToAuth={() => setActiveMenu('auth')}
        />

        <main className="content-body">
          {isAuthChecking ? (
            <div className="section-card" style={{ display: 'flex', alignItems: 'center', gap: '12px', maxWidth: '520px' }}>
              <div className="loading-spinner" />
              <span style={{ fontWeight: 600 }}>세션 상태를 확인하고 있습니다...</span>
            </div>
          ) : (
            <>
              {activeMenu === 'auth' && (
                currentUser ? (
                  <div className="section-card account-card">
                    <div className="section-header">
                      <div>
                        <h2 className="section-title">내 계정</h2>
                        <p style={{ fontSize: '0.825rem', color: 'var(--text-muted)', marginTop: '4px' }}>
                          현재 세션에 로그인된 사용자 정보입니다.
                        </p>
                      </div>
                      <button className="btn btn-secondary" onClick={handleLogout}>로그아웃</button>
                    </div>
                    <div className="detail-meta" style={{ gridTemplateColumns: 'repeat(3, 1fr)' }}>
                      <div className="meta-item">
                        <span>이름</span>
                        <p>{currentUser.name}</p>
                      </div>
                      <div className="meta-item">
                        <span>이메일</span>
                        <p>{currentUser.email}</p>
                      </div>
                      <div className="meta-item">
                        <span>권한</span>
                        <p>{currentUser.role}</p>
                      </div>
                    </div>
                  </div>
                ) : (
                  <AuthView onLogin={handleLogin} onSignup={handleSignup} />
                )
              )}

              {activeMenu === 'dashboard' && (
                currentUser ? (
                  <DashboardView
                    totalElement={totalElement}
                    boards={boards}
                    onViewDetail={handleViewDetail}
                    onNavigateToBoard={() => handleMenuChange('board')}
                  />
                ) : (
                  renderProtectedNotice('대시보드와 게시글 통계를 보려면 먼저 로그인해 주세요.')
                )
              )}

              {activeMenu === 'board' && (
                currentUser ? (
                  <>
                    {boardView === 'list' && (
                      <BoardListView
                        boards={boards}
                        pageInfo={pageInfo}
                        onPageChange={setCurrentPage}
                        onNavigateToWrite={() => setBoardView('write')}
                        onViewDetail={handleViewDetail}
                      />
                    )}
                    {boardView === 'detail' && (
                      <BoardDetailView
                        board={selectedBoard}
                        isLoading={isDetailLoading}
                        onNavigateToList={() => setBoardView('list')}
                        onNavigateToEdit={() => setBoardView('edit')}
                        onDelete={handleDeleteBoard}
                      />
                    )}
                    {boardView === 'write' && (
                      <BoardWriteView
                        onSubmit={handleCreateBoard}
                        onNavigateToList={() => setBoardView('list')}
                      />
                    )}
                    {boardView === 'edit' && (
                      <BoardEditView
                        board={selectedBoard}
                        isLoading={isDetailLoading}
                        onSubmit={handleUpdateBoard}
                        onNavigateToDetail={() => setBoardView('detail')}
                      />
                    )}
                  </>
                ) : (
                  renderProtectedNotice('게시글 목록을 조회하거나 새 글을 작성하려면 로그인해 주세요.')
                )
              )}


              {activeMenu === 'chess' && (
                currentUser ? (
                  <ChessAnalysisView
                    onToast={addToast}
                    onSessionExpired={handleSessionExpired}
                  />
                ) : (
                  renderProtectedNotice('체스 PGN 분석을 실행하려면 먼저 로그인해 주세요.')
                )
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
                        <strong style={{ display: 'block', fontSize: '0.95rem' }}>인증 방식</strong>
                        <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Spring Security Session + JSESSIONID</span>
                      </div>
                      <span style={{ fontSize: '0.9rem', fontWeight: 600 }}>{currentUser ? '로그인됨' : '비로그인'}</span>
                    </div>
                  </div>
                </div>
              )}
            </>
          )}
        </main>
      </div>

      {/* Dynamic Toast Container */}
      <ToastContainer toasts={toasts} />
    </div>
  );
}

export default App;
