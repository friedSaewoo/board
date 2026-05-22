import React from 'react';
import { Board } from '../types';

interface DashboardViewProps {
  totalElement: number;
  boards: Board[];
  onViewDetail: (id: number) => void;
  onNavigateToBoard: () => void;
}

export const DashboardView: React.FC<DashboardViewProps> = ({
  totalElement,
  boards,
  onViewDetail,
  onNavigateToBoard,
}) => {
  // 오늘 작성된 게시글 수 계산
  const getTodayPostsCount = () => {
    const today = new Date();
    const yyyy = today.getFullYear();
    const mm = String(today.getMonth() + 1).padStart(2, '0');
    const dd = String(today.getDate()).padStart(2, '0');
    const todayStr = `${yyyy}-${mm}-${dd}`;
    
    return boards.filter((board) => {
      if (!board.createdAt) return false;
      return board.createdAt.startsWith(todayStr);
    }).length;
  };

  // 최근 게시글 5개 추출
  const recentBoards = boards.slice(0, 5);

  const formatDate = (dateStr: string) => {
    if (!dateStr) return '-';
    const date = new Date(dateStr);
    return `${date.getFullYear()}.${String(date.getMonth() + 1).padStart(2, '0')}.${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
  };

  return (
    <div>
      {/* Metrics Section */}
      <div className="dashboard-grid">
        <div className="metric-card">
          <div className="metric-info">
            <h3>전체 게시글 수</h3>
            <p>{totalElement}개</p>
          </div>
          <div className="metric-icon">📂</div>
        </div>

        <div className="metric-card">
          <div className="metric-info">
            <h3>오늘 등록된 새 글</h3>
            <p>{getTodayPostsCount()}개</p>
          </div>
          <div className="metric-icon" style={{ backgroundColor: 'rgba(16, 185, 129, 0.1)', color: 'var(--success)' }}>
            ✍️
          </div>
        </div>

        <div className="metric-card">
          <div className="metric-info">
            <h3>시스템 상태</h3>
            <p>정상 작동중</p>
          </div>
          <div className="metric-icon" style={{ backgroundColor: 'rgba(59, 130, 246, 0.1)', color: '#3b82f6' }}>
            🟢
          </div>
        </div>
      </div>

      {/* Main Grid for recent items & stats */}
      <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: '24px' }}>
        
        {/* Recent Posts Table */}
        <div className="section-card" style={{ marginBottom: 0 }}>
          <div className="section-header">
            <h2 className="section-title">최근 등록된 게시글</h2>
            <button className="btn btn-secondary" onClick={onNavigateToBoard} style={{ padding: '6px 12px', fontSize: '0.8rem' }}>
              전체 보기 →
            </button>
          </div>

          <div className="table-container">
            <table className="admin-table">
              <thead>
                <tr>
                  <th style={{ width: '80px' }}>ID</th>
                  <th>제목</th>
                  <th style={{ width: '160px' }}>등록일</th>
                </tr>
              </thead>
              <tbody>
                {recentBoards.length === 0 ? (
                  <tr>
                    <td colSpan={3} style={{ textAlign: 'center', color: 'var(--text-muted)', padding: '30px' }}>
                      최근 등록된 게시글이 없습니다.
                    </td>
                  </tr>
                ) : (
                  recentBoards.map((board) => (
                    <tr key={board.boardId} onClick={() => onViewDetail(board.boardId)}>
                      <td style={{ fontWeight: 600, color: 'var(--text-muted)' }}>#{board.boardId}</td>
                      <td style={{ fontWeight: 500 }}>{board.title}</td>
                      <td style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>{formatDate(board.createdAt)}</td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>

        {/* System & Info Card */}
        <div className="section-card" style={{ marginBottom: 0, display: 'flex', flexDirection: 'column', gap: '20px' }}>
          <h2 className="section-title">시스템 정보</h2>
          
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            <div style={{ padding: '12px', borderRadius: '8px', border: '1px solid var(--border-color)', backgroundColor: 'hsl(var(--background-h), var(--background-s), calc(var(--background-l) - 1%))' }}>
              <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontWeight: 600, display: 'block', marginBottom: '4px' }}>SERVER STACK</span>
              <span style={{ fontSize: '0.875rem', fontWeight: 700 }}>Spring Boot 3.5.14 & Java 17</span>
            </div>
            
            <div style={{ padding: '12px', borderRadius: '8px', border: '1px solid var(--border-color)', backgroundColor: 'hsl(var(--background-h), var(--background-s), calc(var(--background-l) - 1%))' }}>
              <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontWeight: 600, display: 'block', marginBottom: '4px' }}>DATABASE</span>
              <span style={{ fontSize: '0.875rem', fontWeight: 700 }}>MySQL (JPA Hibernate)</span>
            </div>

            <div style={{ padding: '12px', borderRadius: '8px', border: '1px solid var(--border-color)', backgroundColor: 'hsl(var(--background-h), var(--background-s), calc(var(--background-l) - 1%))' }}>
              <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontWeight: 600, display: 'block', marginBottom: '4px' }}>FRONTEND FRAMEWORK</span>
              <span style={{ fontSize: '0.875rem', fontWeight: 700 }}>React 18 & Vite (TS)</span>
            </div>
          </div>

          <div style={{ marginTop: 'auto', padding: '16px', borderRadius: '8px', background: 'linear-gradient(135deg, var(--primary) 0%, hsl(var(--primary-h), var(--primary-s), 45%) 100%)', color: 'white', display: 'flex', flexDirection: 'column', gap: '8px' }}>
            <span style={{ fontWeight: 700, fontSize: '0.9rem' }}>💡 팁</span>
            <p style={{ fontSize: '0.75rem', opacity: 0.9, lineHeight: 1.4 }}>
              상단 우측의 테마 변경 버튼을 클릭하여 다크 모드를 체험해 보세요. 어드민 페이지에 특화된 색조 편안함을 제공합니다.
            </p>
          </div>
        </div>

      </div>
    </div>
  );
};
