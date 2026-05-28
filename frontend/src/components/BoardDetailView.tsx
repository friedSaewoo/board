import React, { useState } from 'react';
import { Board, Toast } from '../types';

interface BoardDetailViewProps {
  board: Board | null;
  isLoading: boolean;
  onNavigateToList: () => void;
  onNavigateToEdit: (id: number) => void;
  onDelete: (id: number) => void;
  onToast: (message: string, type: Toast['type']) => void;
}

export const BoardDetailView: React.FC<BoardDetailViewProps> = ({
  board,
  isLoading,
  onNavigateToList,
  onNavigateToEdit,
  onDelete,
  onToast,
}) => {
  const [isCopied, setIsCopied] = useState(false);

  const formatDate = (dateStr: string) => {
    if (!dateStr) return '-';
    const date = new Date(dateStr);
    return `${date.getFullYear()}.${String(date.getMonth() + 1).padStart(2, '0')}.${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
  };

  const handleDeleteClick = () => {
    if (board && window.confirm('정말로 이 게시글을 삭제하시겠습니까?')) {
      onDelete(board.boardId);
    }
  };

  const isChessPromptPost = board?.title?.startsWith('[체스 분석]');

  const handleCopyContents = async () => {
    if (!board?.contents) return;

    try {
      if (navigator.clipboard?.writeText) {
        await navigator.clipboard.writeText(board.contents);
      } else {
        const textarea = document.createElement('textarea');
        textarea.value = board.contents;
        textarea.style.position = 'fixed';
        textarea.style.opacity = '0';
        document.body.appendChild(textarea);
        textarea.focus();
        textarea.select();
        document.execCommand('copy');
        document.body.removeChild(textarea);
      }

      setIsCopied(true);
      onToast(isChessPromptPost ? 'AI 프롬프트를 복사했습니다.' : '게시글 본문을 복사했습니다.', 'success');
      window.setTimeout(() => setIsCopied(false), 1800);
    } catch (err) {
      console.error(err);
      onToast('복사에 실패했습니다. 본문을 직접 선택해 복사해 주세요.', 'error');
    }
  };

  if (isLoading) {
    return (
      <div className="section-card" style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', padding: '100px 0' }}>
        <div className="loading-spinner"></div>
        <p style={{ fontSize: '0.95rem', color: 'var(--text-muted)', marginTop: '16px' }}>게시글을 불러오고 있습니다...</p>
      </div>
    );
  }

  if (!board) {
    return (
      <div className="section-card" style={{ textAlign: 'center', padding: '60px 0' }}>
        <p style={{ color: 'var(--text-muted)', marginBottom: '20px' }}>요청하신 게시물을 찾을 수 없습니다.</p>
        <button className="btn btn-secondary" onClick={onNavigateToList}>
          목록으로 돌아가기
        </button>
      </div>
    );
  }

  return (
    <div className="section-card" style={{ maxWidth: '900px' }}>
      <div className="section-header" style={{ borderBottom: '1px solid var(--border-color)', paddingBottom: '20px', marginBottom: '24px' }}>
        <div>
          <h2 style={{ fontSize: '1.5rem', fontWeight: 800, letterSpacing: '-0.5px', marginBottom: '8px' }}>
            {board.title}
          </h2>
          <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
            게시글 ID: <strong>#{board.boardId}</strong>
          </span>
        </div>
        <button className="btn btn-secondary" type="button" onClick={handleCopyContents}>
          {isCopied ? '✅ 복사 완료' : isChessPromptPost ? '📋 프롬프트 복사' : '📋 본문 복사'}
        </button>
      </div>

      <div className="detail-meta" style={{ marginBottom: '24px', gridTemplateColumns: 'repeat(3, 1fr)' }}>
        <div className="meta-item">
          <span>작성자</span>
          <p>관리자 (Admin)</p>
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

      <div style={{ minHeight: '250px', padding: '16px 8px', fontSize: '1.05rem', lineHeight: 1.7, borderBottom: '1px solid var(--border-color)', marginBottom: '24px' }}>
        <div className="detail-body">
          {board.contents}
        </div>
      </div>

      <div style={{ display: 'flex', gap: '12px', justifyContent: 'space-between' }}>
        <button className="btn btn-secondary" onClick={onNavigateToList}>
          ← 목록으로
        </button>
        <div style={{ display: 'flex', gap: '12px' }}>
          <button className="btn btn-danger" onClick={handleDeleteClick}>
            🗑️ 삭제하기
          </button>
          <button className="btn btn-primary" onClick={() => onNavigateToEdit(board.boardId)}>
            ✏️ 수정하기
          </button>
        </div>
      </div>
    </div>
  );
};
