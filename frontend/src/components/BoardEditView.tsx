import React, { useState, useEffect } from 'react';
import { Board } from '../types';

interface BoardEditViewProps {
  board: Board | null;
  isLoading: boolean;
  onSubmit: (title: string, contents: string) => Promise<boolean>;
  onNavigateToDetail: () => void;
}

export const BoardEditView: React.FC<BoardEditViewProps> = ({
  board,
  isLoading,
  onSubmit,
  onNavigateToDetail,
}) => {
  const [title, setTitle] = useState('');
  const [contents, setContents] = useState('');
  const [error, setError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  // 데이터 로드 시 폼 초기화
  useEffect(() => {
    if (board) {
      setTitle(board.title);
      setContents(board.contents);
      setError('');
    }
  }, [board]);

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
        <p style={{ color: 'var(--text-muted)', marginBottom: '20px' }}>수정 대상 게시글을 찾을 수 없습니다.</p>
        <button className="btn btn-secondary" onClick={onNavigateToDetail}>
          돌아가기
        </button>
      </div>
    );
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!title.trim()) {
      setError('제목을 입력해 주세요.');
      return;
    }
    if (!contents.trim()) {
      setError('내용을 입력해 주세요.');
      return;
    }

    // 변경사항 유무 체크 (프론트 단에서 요청 방어)
    if (title.trim() === board.title && contents.trim() === board.contents) {
      setError('변경된 내용이 없습니다.');
      return;
    }

    setError('');
    setIsSubmitting(true);

    try {
      const success = await onSubmit(title.trim(), contents.trim());
      if (success) {
        onNavigateToDetail();
      }
    } catch (err) {
      setError('게시글 수정에 실패했습니다. 서버 상태를 확인해 주세요.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="section-card" style={{ maxWidth: '800px' }}>
      <div className="section-header" style={{ borderBottom: '1px solid var(--border-color)', paddingBottom: '16px', marginBottom: '24px' }}>
        <div>
          <h2 className="section-title" style={{ fontSize: '1.25rem' }}>게시글 수정</h2>
          <p style={{ fontSize: '0.825rem', color: 'var(--text-muted)', marginTop: '4px' }}>
            게시글 ID: <strong>#{board.boardId}</strong> 번 글의 수정 화면입니다.
          </p>
        </div>
      </div>

      <form onSubmit={handleSubmit}>
        {error && (
          <div style={{
            padding: '12px 16px',
            backgroundColor: 'rgba(239, 68, 68, 0.1)',
            color: '#ef4444',
            borderRadius: '8px',
            fontSize: '0.85rem',
            fontWeight: 600,
            marginBottom: '24px',
            border: '1px solid rgba(239, 68, 68, 0.2)'
          }}>
            ⚠️ {error}
          </div>
        )}

        <div className="form-group">
          <label className="form-label" htmlFor="edit-title">제목</label>
          <input
            type="text"
            id="edit-title"
            className="form-input"
            placeholder="제목을 입력하세요 (최대 100자)"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            disabled={isSubmitting}
            maxLength={100}
          />
        </div>

        <div className="form-group" style={{ marginBottom: '28px' }}>
          <label className="form-label" htmlFor="edit-contents">내용</label>
          <textarea
            id="edit-contents"
            className="form-input form-textarea"
            placeholder="내용을 입력하세요 (최대 50000자)"
            value={contents}
            onChange={(e) => setContents(e.target.value)}
            disabled={isSubmitting}
            maxLength={50000}
            style={{ minHeight: '220px' }}
          />
        </div>

        <div style={{ display: 'flex', justifyContent: 'space-between', borderTop: '1px solid var(--border-color)', paddingTop: '20px' }}>
          <button
            type="button"
            className="btn btn-secondary"
            onClick={onNavigateToDetail}
            disabled={isSubmitting}
          >
            ← 취소
          </button>
          <button
            type="submit"
            className="btn btn-primary"
            disabled={isSubmitting}
            style={{ display: 'inline-flex', alignItems: 'center', gap: '8px' }}
          >
            {isSubmitting ? (
              <>
                <div className="loading-spinner" style={{ width: '14px', height: '14px', borderWidth: '2px' }}></div>
                수정 중...
              </>
            ) : (
              '수정하기'
            )}
          </button>
        </div>
      </form>
    </div>
  );
};
