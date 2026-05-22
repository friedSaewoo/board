import React from 'react';
import { Board } from '../types';

interface BoardDetailModalProps {
  isOpen: boolean;
  board: Board | null;
  isLoading: boolean;
  onClose: () => void;
}

export const BoardDetailModal: React.FC<BoardDetailModalProps> = ({
  isOpen,
  board,
  isLoading,
  onClose,
}) => {
  if (!isOpen) return null;

  const formatDate = (dateStr: string) => {
    if (!dateStr) return '-';
    const date = new Date(dateStr);
    return `${date.getFullYear()}.${String(date.getMonth() + 1).padStart(2, '0')}.${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h3 className="modal-title">게시물 상세 정보</h3>
          <button className="modal-close" onClick={onClose} aria-label="Close modal">
            &times;
          </button>
        </div>

        {isLoading ? (
          <div style={{ padding: '40px 0', textAlign: 'center', color: 'var(--text-muted)' }}>
            <div className="loading-spinner"></div>
            <p style={{ fontSize: '0.9rem', marginTop: '12px' }}>데이터를 불러오는 중입니다...</p>
          </div>
        ) : board ? (
          <div>
            <h2 style={{ fontSize: '1.35rem', fontWeight: 800, marginBottom: '20px', letterSpacing: '-0.5px', lineHeight: 1.4 }}>
              {board.title}
            </h2>

            <div className="detail-meta">
              <div className="meta-item">
                <span>글 번호</span>
                <p>#{board.boardId}</p>
              </div>
              <div className="meta-item">
                <span>등록 시간</span>
                <p>{formatDate(board.createdAt)}</p>
              </div>
              <div className="meta-item">
                <span>수정 시간</span>
                <p>{formatDate(board.updatedAt)}</p>
              </div>
            </div>

            <div style={{ borderTop: '1px solid var(--border-color)', paddingTop: '20px', marginTop: '10px' }}>
              <div className="detail-body">
                {board.contents}
              </div>
            </div>
          </div>
        ) : (
          <p style={{ textAlign: 'center', padding: '20px', color: 'var(--text-muted)' }}>
            게시글을 찾을 수 없습니다.
          </p>
        )}

        <div className="modal-footer">
          <button className="btn btn-secondary" onClick={onClose}>
            닫기
          </button>
        </div>
      </div>
    </div>
  );
};
