import React from 'react';
import { Board, PageInfo } from '../types';

interface BoardListViewProps {
  boards: Board[];
  pageInfo: PageInfo | null;
  onPageChange: (page: number) => void;
  onNavigateToWrite: () => void;
  onViewDetail: (id: number) => void;
}

export const BoardListView: React.FC<BoardListViewProps> = ({
  boards,
  pageInfo,
  onPageChange,
  onNavigateToWrite,
  onViewDetail,
}) => {
  const formatDate = (dateStr: string) => {
    if (!dateStr) return '-';
    const date = new Date(dateStr);
    return `${date.getFullYear()}.${String(date.getMonth() + 1).padStart(2, '0')}.${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
  };

  // 페이징 번호 배열 생성
  const getPageNumbers = () => {
    if (!pageInfo) return [];
    const numbers = [];
    for (let i = pageInfo.blockStart; i <= pageInfo.blockEnd; i++) {
      numbers.push(i);
    }
    return numbers;
  };

  return (
    <div className="section-card">
      <div className="section-header">
        <div>
          <h2 className="section-title">게시물 목록</h2>
          <p style={{ fontSize: '0.825rem', color: 'var(--text-muted)', marginTop: '4px' }}>
            등록된 게시물을 조회하고 신규 글을 작성할 수 있습니다.
          </p>
        </div>
        <button className="btn btn-primary" onClick={onNavigateToWrite}>
          <span>+</span> 새 글 작성
        </button>
      </div>

      <div className="table-container">
        <table className="admin-table">
          <thead>
            <tr>
              <th style={{ width: '80px' }}>ID</th>
              <th>제목</th>
              <th style={{ width: '180px' }}>등록일</th>
              <th style={{ width: '180px' }}>수정일</th>
            </tr>
          </thead>
          <tbody>
            {boards.length === 0 ? (
              <tr>
                <td colSpan={4} style={{ textAlign: 'center', color: 'var(--text-muted)', padding: '40px' }}>
                  등록된 게시글이 없습니다. 첫 번째 글을 작성해 보세요!
                </td>
              </tr>
            ) : (
              boards.map((board) => (
                <tr key={board.boardId} onClick={() => onViewDetail(board.boardId)}>
                  <td style={{ fontWeight: 600, color: 'var(--text-muted)' }}>#{board.boardId}</td>
                  <td style={{ fontWeight: 600 }}>{board.title}</td>
                  <td style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>{formatDate(board.createdAt)}</td>
                  <td style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>{formatDate(board.updatedAt)}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Pagination control panel */}
      {pageInfo && pageInfo.totalPage > 0 && (
        <div className="pagination">
          <span style={{ fontSize: '0.825rem', color: 'var(--text-muted)' }}>
            총 <strong>{pageInfo.totalElement}</strong>개 중 {boards.length}개 표시 (페이지 {pageInfo.pageNum}/{pageInfo.totalPage})
          </span>
          <div className="pagination-buttons">
            {/* 이전 블록 */}
            <button
              className="pagination-btn"
              disabled={!pageInfo.hasPrev}
              onClick={() => onPageChange(pageInfo.blockStart - 1)}
              title="이전 페이지 블록"
            >
              &lt;
            </button>

            {/* 페이지 번호들 */}
            {getPageNumbers().map((num) => (
              <button
                key={num}
                className={`pagination-btn ${pageInfo.pageNum === num ? 'active' : ''}`}
                onClick={() => onPageChange(num)}
              >
                {num}
              </button>
            ))}

            {/* 다음 블록 */}
            <button
              className="pagination-btn"
              disabled={!pageInfo.hasNext}
              onClick={() => onPageChange(pageInfo.blockEnd + 1)}
              title="다음 페이지 블록"
            >
              &gt;
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
