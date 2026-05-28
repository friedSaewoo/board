import React, { useState } from 'react';

interface BoardWriteViewProps {
  onSubmit: (title: string, contents: string) => Promise<boolean>;
  onNavigateToList: () => void;
}

export const BoardWriteView: React.FC<BoardWriteViewProps> = ({
  onSubmit,
  onNavigateToList,
}) => {
  const [title, setTitle] = useState('');
  const [contents, setContents] = useState('');
  const [error, setError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

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

    setError('');
    setIsSubmitting(true);

    try {
      const success = await onSubmit(title.trim(), contents.trim());
      if (success) {
        onNavigateToList();
      }
    } catch (err) {
      setError('게시글 등록에 실패했습니다. 서버 상태를 확인해 주세요.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="section-card" style={{ maxWidth: '800px' }}>
      <div className="section-header" style={{ borderBottom: '1px solid var(--border-color)', paddingBottom: '16px', marginBottom: '24px' }}>
        <div>
          <h2 className="section-title" style={{ fontSize: '1.25rem' }}>새 게시글 등록</h2>
          <p style={{ fontSize: '0.825rem', color: 'var(--text-muted)', marginTop: '4px' }}>
            새로운 소식이나 정보를 입력하여 등록해 보세요.
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
          <label className="form-label" htmlFor="board-title">제목</label>
          <input
            type="text"
            id="board-title"
            className="form-input"
            placeholder="제목을 입력하세요 (최대 100자)"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            disabled={isSubmitting}
            maxLength={100}
          />
        </div>

        <div className="form-group" style={{ marginBottom: '28px' }}>
          <label className="form-label" htmlFor="board-contents">내용</label>
          <textarea
            id="board-contents"
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
            onClick={onNavigateToList}
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
                등록 중...
              </>
            ) : (
              '등록하기'
            )}
          </button>
        </div>
      </form>
    </div>
  );
};
