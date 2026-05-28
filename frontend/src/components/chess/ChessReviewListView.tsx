import React, { useEffect, useState } from 'react';
import { ChessReviewSummary, PageInfo, PagedResult, Toast } from '../../types';

interface ChessReviewListViewProps {
  onToast: (message: string, type: Toast['type']) => void;
  onSessionExpired: () => void;
  onOpenReview: (id: number) => void;
}

export const ChessReviewListView: React.FC<ChessReviewListViewProps> = ({ onToast, onSessionExpired, onOpenReview }) => {
  const [reviews, setReviews] = useState<ChessReviewSummary[]>([]);
  const [pageInfo, setPageInfo] = useState<PageInfo | null>(null);
  const [page, setPage] = useState(1);
  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => {
    void fetchReviews(page);
  }, [page]);

  const fetchReviews = async (targetPage: number) => {
    setIsLoading(true);
    try {
      const response = await fetch(`/chess/reviews?pageNum=${targetPage}&pageSize=10&sortBy=id&direction=DESC`, {
        credentials: 'include',
      });
      if (response.status === 401 || response.status === 403) {
        onSessionExpired();
        throw new Error('로그인 세션이 만료되었습니다.');
      }
      if (!response.ok) {
        throw new Error('체스 리뷰 목록을 불러오지 못했습니다.');
      }
      const data = await response.json() as PagedResult<ChessReviewSummary>;
      setReviews(data.content || []);
      setPageInfo(data.pageInfo || null);
    } catch (err) {
      console.error(err);
      onToast(err instanceof Error ? err.message : '체스 리뷰 목록 조회 중 오류가 발생했습니다.', 'error');
    } finally {
      setIsLoading(false);
    }
  };

  const handleDelete = async (id: number) => {
    try {
      const response = await fetch(`/chess/reviews/${id}`, {
        method: 'DELETE',
        credentials: 'include',
      });
      if (response.status === 401 || response.status === 403) {
        onSessionExpired();
        throw new Error('로그인 세션이 만료되었습니다.');
      }
      if (!response.ok) {
        throw new Error('체스 리뷰 삭제에 실패했습니다.');
      }
      onToast('체스 리뷰를 삭제했습니다.', 'success');
      await fetchReviews(page);
    } catch (err) {
      console.error(err);
      onToast(err instanceof Error ? err.message : '체스 리뷰 삭제 중 오류가 발생했습니다.', 'error');
    }
  };

  return (
    <section className="section-card">
      <div className="section-header">
        <div>
          <h2 className="section-title">체스 리뷰 게시판</h2>
          <p className="chess-helper-text">외부 AI 응답까지 붙여넣어 생성한 개인 체스 리뷰만 별도로 표시합니다.</p>
        </div>
      </div>
      <div className="table-container">
        <table className="admin-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>제목</th>
              <th>대국자</th>
              <th>결과</th>
              <th>선택 색</th>
              <th>수</th>
              <th>작성일</th>
              <th>작업</th>
            </tr>
          </thead>
          <tbody>
            {isLoading ? (
              <tr><td colSpan={8} style={{ textAlign: 'center', padding: '32px' }}>불러오는 중...</td></tr>
            ) : reviews.length === 0 ? (
              <tr><td colSpan={8} style={{ textAlign: 'center', padding: '32px', color: 'var(--text-muted)' }}>저장된 체스 리뷰가 없습니다.</td></tr>
            ) : reviews.map((review) => (
              <tr key={review.id}>
                <td>#{review.id}</td>
                <td><button className="link-button" type="button" onClick={() => onOpenReview(review.id)}>{review.title}</button></td>
                <td>{review.whiteName || 'White'} vs {review.blackName || 'Black'}</td>
                <td>{review.result || '-'}</td>
                <td>{review.playerColor === 'WHITE' ? '백' : '흑'}</td>
                <td>{review.moveCount}</td>
                <td>{new Date(review.createdAt).toLocaleString()}</td>
                <td className="table-actions">
                  <button className="btn btn-secondary" type="button" onClick={() => onOpenReview(review.id)}>열기</button>
                  <button className="btn btn-danger" type="button" onClick={() => void handleDelete(review.id)}>삭제</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {pageInfo && pageInfo.totalPage > 1 && (
        <div className="pagination">
          <button className="btn btn-secondary" disabled={!pageInfo.hasPrev} onClick={() => setPage((prev) => Math.max(1, prev - 1))}>이전</button>
          <span>{pageInfo.pageNum} / {pageInfo.totalPage}</span>
          <button className="btn btn-secondary" disabled={!pageInfo.hasNext} onClick={() => setPage((prev) => prev + 1)}>다음</button>
        </div>
      )}
    </section>
  );
};
