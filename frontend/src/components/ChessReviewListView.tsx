import React from 'react';
import { ChessReviewSummary, PageInfo } from '../types';

interface ChessReviewListViewProps {
  reviews: ChessReviewSummary[];
  pageInfo: PageInfo | null;
  isLoading: boolean;
  onPageChange: (page: number) => void;
  onViewDetail: (id: number) => void;
  onDelete: (id: number) => void;
  onNavigateToAnalysis: () => void;
}

const formatDate = (dateStr: string) => {
  if (!dateStr) return '-';
  const date = new Date(dateStr);
  if (Number.isNaN(date.getTime())) return '-';
  return `${date.getFullYear()}.${String(date.getMonth() + 1).padStart(2, '0')}.${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
};

const sideLabel = (side: ChessReviewSummary['playerColor']) => (side === 'WHITE' ? '백' : side === 'BLACK' ? '흑' : '-');

const gameLabel = (review: ChessReviewSummary) => {
  const white = review.whiteName?.trim() || 'White';
  const black = review.blackName?.trim() || 'Black';
  return `${white} vs ${black}`;
};

export const ChessReviewListView: React.FC<ChessReviewListViewProps> = ({
  reviews,
  pageInfo,
  isLoading,
  onPageChange,
  onViewDetail,
  onDelete,
  onNavigateToAnalysis,
}) => {
  const getPageNumbers = () => {
    if (!pageInfo) return [];
    const numbers = [];
    for (let i = pageInfo.blockStart; i <= pageInfo.blockEnd; i += 1) {
      numbers.push(i);
    }
    return numbers;
  };

  return (
    <div className="section-card chess-review-list-card">
      <div className="section-header">
        <div>
          <h2 className="section-title">체스 리뷰 게시판</h2>
          <p className="chess-helper-text">
            Stockfish 분석 후 외부 AI 응답을 붙여넣어 생성한 체스 리뷰만 별도로 표시합니다.
          </p>
        </div>
        <button className="btn btn-primary" type="button" onClick={onNavigateToAnalysis}>
          ♟️ 새 체스 분석
        </button>
      </div>

      <div className="table-container">
        <table className="admin-table chess-review-table">
          <thead>
            <tr>
              <th style={{ width: '80px' }}>ID</th>
              <th>리뷰</th>
              <th style={{ width: '110px' }}>플레이 색</th>
              <th style={{ width: '90px' }}>결과</th>
              <th style={{ width: '90px' }}>수</th>
              <th style={{ width: '170px' }}>생성일</th>
              <th style={{ width: '130px' }}>관리</th>
            </tr>
          </thead>
          <tbody>
            {isLoading ? (
              <tr>
                <td colSpan={7} className="chess-empty-cell">
                  <div className="loading-spinner" /> 체스 리뷰를 불러오는 중입니다...
                </td>
              </tr>
            ) : reviews.length === 0 ? (
              <tr>
                <td colSpan={7} className="chess-empty-cell">
                  아직 생성된 체스 리뷰가 없습니다. 분석 결과에 AI 응답을 붙여넣어 첫 리뷰를 만들어 보세요.
                </td>
              </tr>
            ) : (
              reviews.map((review) => (
                <tr key={review.id} onClick={() => onViewDetail(review.id)}>
                  <td style={{ fontWeight: 600, color: 'var(--text-muted)' }}>#{review.id}</td>
                  <td>
                    <strong>{review.title || gameLabel(review)}</strong>
                    <small className="chess-subtext">{gameLabel(review)}</small>
                  </td>
                  <td><span className="chess-pill">{sideLabel(review.playerColor)}</span></td>
                  <td>{review.result || '-'}</td>
                  <td>{review.moveCount}</td>
                  <td style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>{formatDate(review.createdAt)}</td>
                  <td>
                    <div className="chess-review-actions" onClick={(event) => event.stopPropagation()}>
                      <button className="btn btn-secondary btn-compact" type="button" onClick={() => onViewDetail(review.id)}>
                        열기
                      </button>
                      <button className="btn btn-danger btn-compact" type="button" onClick={() => onDelete(review.id)}>
                        삭제
                      </button>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {pageInfo && pageInfo.totalPage > 0 && (
        <div className="pagination">
          <span style={{ fontSize: '0.825rem', color: 'var(--text-muted)' }}>
            총 <strong>{pageInfo.totalElement}</strong>개 중 {reviews.length}개 표시 (페이지 {pageInfo.pageNum}/{pageInfo.totalPage})
          </span>
          <div className="pagination-buttons">
            <button
              className="pagination-btn"
              disabled={!pageInfo.hasPrev}
              onClick={() => onPageChange(pageInfo.blockStart - 1)}
              title="이전 페이지 블록"
            >
              &lt;
            </button>
            {getPageNumbers().map((num) => (
              <button
                key={num}
                className={`pagination-btn ${pageInfo.pageNum === num ? 'active' : ''}`}
                onClick={() => onPageChange(num)}
              >
                {num}
              </button>
            ))}
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
