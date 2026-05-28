import { useEffect, useState } from 'react';
import { deleteChessReview, fetchChessReviews } from './api/chessReviews';
import { AuthView } from './components/AuthView';
import { BoardDetailView } from './components/BoardDetailView';
import { BoardEditView } from './components/BoardEditView';
import { BoardListView } from './components/BoardListView';
import { BoardWriteView } from './components/BoardWriteView';
import { ChessAnalysisView } from './components/ChessAnalysisView';
import { ChessReviewListView } from './components/ChessReviewListView';
import { DashboardView } from './components/DashboardView';
import { Header } from './components/Header';
import { Sidebar } from './components/Sidebar';
import { ToastContainer } from './components/ToastContainer';
import { ActiveMenu, Board, ChessReviewSummary, Member, PageInfo, Toast } from './types';

function App() {
  const [activeMenu, setActiveMenu] = useState<ActiveMenu>('auth');
  const [theme, setTheme] = useState<'light' | 'dark'>('light');

  const [currentUser, setCurrentUser] = useState<Member | null>(null);
  const [isAuthChecking, setIsAuthChecking] = useState(true);

  const [boards, setBoards] = useState<Board[]>([]);
  const [pageInfo, setPageInfo] = useState<PageInfo | null>(null);
  const [currentPage, setCurrentPage] = useState<number>(1);
  const [totalElement, setTotalElement] = useState<number>(0);

  const [boardView, setBoardView] = useState<'list' | 'detail' | 'write' | 'edit'>('list');
  const [selectedBoard, setSelectedBoard] = useState<Board | null>(null);
  const [isDetailLoading, setIsDetailLoading] = useState(false);
  const [chessReviewView, setChessReviewView] = useState<'list' | 'detail'>('list');
  const [selectedChessReviewId, setSelectedChessReviewId] = useState<number | null>(null);

  const [chessReviews, setChessReviews] = useState<ChessReviewSummary[]>([]);
  const [chessReviewPageInfo, setChessReviewPageInfo] = useState<PageInfo | null>(null);
  const [chessReviewCurrentPage, setChessReviewCurrentPage] = useState<number>(1);
  const [chessReviewView, setChessReviewView] = useState<'list' | 'detail'>('list');
  const [selectedChessReviewId, setSelectedChessReviewId] = useState<number | null>(null);
  const [isChessReviewLoading, setIsChessReviewLoading] = useState(false);

  const [toasts, setToasts] = useState<Toast[]>([]);

  useEffect(() => {
    const savedTheme = localStorage.getItem('theme') as 'light' | 'dark' | null;
    const systemPrefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
    const initialTheme = savedTheme || (systemPrefersDark ? 'dark' : 'light');

    setTheme(initialTheme);
    document.documentElement.setAttribute('data-theme', initialTheme);
  }, []);

  useEffect(() => {
    checkCurrentUser();
  }, []);

  useEffect(() => {
    if (currentUser) {
      fetchBoards(currentPage);
      return;
    }

    setBoards([]);
    setPageInfo(null);
    setTotalElement(0);
  }, [currentPage, currentUser]);

  useEffect(() => {
    if (currentUser && activeMenu === 'chessReviews' && chessReviewView === 'list') {
      fetchChessReviewList(chessReviewCurrentPage);
      return;
    }

    if (!currentUser) {
      setChessReviews([]);
      setChessReviewPageInfo(null);
    }
  }, [activeMenu, chessReviewCurrentPage, chessReviewView, currentUser]);

  const addToast = (message: string, type: 'success' | 'error' | 'info') => {
    const id = Date.now();
    setToasts((prev) => [...prev, { id, message, type }]);

    setTimeout(() => {
      setToasts((prev) => prev.filter((toast) => toast.id !== id));
    }, 3000);
  };

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

  const handleMenuChange = (menu: ActiveMenu) => {
    setActiveMenu(menu);
    if (menu === 'board') {
      setBoardView('list');
    }
    if (menu === 'chessReviews') {
      setChessReviewView('list');
    }
  };

  const toggleTheme = () => {
    const nextTheme = theme === 'light' ? 'dark' : 'light';
    setTheme(nextTheme);
    document.documentElement.setAttribute('data-theme', nextTheme);
    localStorage.setItem('theme', nextTheme);
    addToast(`${nextTheme === 'light' ? '라이트 모드' : '다크 모드'}로 변경되었습니다.`, 'info');
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

  const resetProtectedState = () => {
    setSelectedBoard(null);
    setSelectedChessReviewId(null);
    setBoardView('list');
    setChessReviews([]);
    setChessReviewPageInfo(null);
    setChessReviewView('list');
    setSelectedChessReviewId(null);
  };

  const handleSessionExpired = () => {
    setCurrentUser(null);
    resetProtectedState();
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
      resetProtectedState();
      setActiveMenu('auth');
      addToast('로그아웃되었습니다.', 'info');
    }
  };

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

  const fetchChessReviewList = async (page: number) => {
    if (!currentUser) return;

    setIsChessReviewLoading(true);
    try {
      const data = await fetchChessReviews(page);
      setChessReviews(data.content || []);
      setChessReviewPageInfo(data.pageInfo || null);
    } catch (err) {
      console.error(err);
      addToast(err instanceof Error ? err.message : '체스 리뷰 목록을 불러오지 못했습니다.', 'error');
      setChessReviews([]);
      setChessReviewPageInfo(null);
    } finally {
      setIsChessReviewLoading(false);
    }
  };

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
      const updatedData = await response.json();
      setSelectedBoard(updatedData);
      fetchBoards(currentPage);
      return true;
    } catch (err) {
      console.error(err);
      addToast('게시글 수정 중 에러가 발생했습니다.', 'error');
      return false;
    }
  };

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
      setBoardView('list');
      fetchBoards(currentPage);
    } catch (err) {
      console.error(err);
      addToast('게시글 삭제 중 에러가 발생했습니다.', 'error');
    }
  };

  const handleChessReviewCreated = (review: ChessReviewSummary) => {
    setSelectedChessReviewId(review.id);
    setChessReviewView('detail');
    setActiveMenu('chessReviews');
    setChessReviewCurrentPage(1);
    fetchChessReviewList(1);
  };

  const handleViewChessReview = (id: number) => {
    if (!currentUser) {
      setActiveMenu('auth');
      addToast('로그인 후 체스 리뷰를 조회할 수 있습니다.', 'info');
      return;
    }

    setSelectedChessReviewId(id);
    setChessReviewView('detail');
    setActiveMenu('chessReviews');
  };

  const handleDeleteChessReview = async (id: number) => {
    if (!currentUser) {
      setActiveMenu('auth');
      addToast('로그인 후 체스 리뷰를 삭제할 수 있습니다.', 'info');
      return;
    }

    if (!window.confirm('이 체스 리뷰를 삭제하시겠습니까?')) {
      return;
    }

    try {
      await deleteChessReview(id);
      addToast('체스 리뷰가 삭제되었습니다.', 'success');
      if (selectedChessReviewId === id) {
        setSelectedChessReviewId(null);
        setChessReviewView('list');
      }
      fetchChessReviewList(chessReviewCurrentPage);
    } catch (err) {
      console.error(err);
      addToast(err instanceof Error ? err.message : '체스 리뷰 삭제 중 오류가 발생했습니다.', 'error');
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
      case 'chessReviews': return chessReviewView === 'detail' ? '체스 리뷰 상세' : '체스 리뷰 게시판';
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

  const renderChessReviewDetailPlaceholder = () => (
    <div className="section-card chess-review-detail-placeholder">
      <div className="section-header">
        <div>
          <h2 className="section-title">체스 리뷰 #{selectedChessReviewId}</h2>
          <p className="chess-helper-text">
            전용 리뷰 보드 경로입니다. 저장된 리뷰 상세/리플레이 보드는 chess.js 리뷰 보드 컴포넌트가 이 화면에 연결됩니다.
          </p>
        </div>
        <div className="chess-review-actions">
          <button className="btn btn-secondary" type="button" onClick={() => setChessReviewView('list')}>
            목록으로
          </button>
          {selectedChessReviewId && (
            <button className="btn btn-danger" type="button" onClick={() => handleDeleteChessReview(selectedChessReviewId)}>
              삭제
            </button>
          )}
        </div>
      </div>
      <p className="chess-helper-text">
        일반 게시판과 분리된 라우팅 및 생성 후 이동이 완료되었습니다. 리뷰 보드 본문은 전용 보드/매칭 작업과 통합됩니다.
      </p>
    </div>
  );

  return (
    <div className="app-container">
      <Sidebar
        activeMenu={activeMenu}
        setActiveMenu={handleMenuChange}
        isAuthenticated={Boolean(currentUser)}
      />

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
                        onToast={addToast}
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
                    onReviewCreated={handleChessReviewCreated}
                  />
                ) : (
                  renderProtectedNotice('체스 PGN 분석을 실행하려면 먼저 로그인해 주세요.')
                )
              )}

              {activeMenu === 'chessReviews' && (
                currentUser ? (
                  chessReviewView === 'list' ? (
                    <ChessReviewListView
                      reviews={chessReviews}
                      pageInfo={chessReviewPageInfo}
                      isLoading={isChessReviewLoading}
                      onPageChange={setChessReviewCurrentPage}
                      onViewDetail={handleViewChessReview}
                      onDelete={handleDeleteChessReview}
                      onNavigateToAnalysis={() => handleMenuChange('chess')}
                    />
                  ) : (
                    renderChessReviewDetailPlaceholder()
                  )
                ) : (
                  renderProtectedNotice('체스 리뷰 게시판을 보려면 먼저 로그인해 주세요.')
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

      <ToastContainer toasts={toasts} />
    </div>
  );
}

export default App;
