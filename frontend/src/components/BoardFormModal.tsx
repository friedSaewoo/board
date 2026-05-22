import React, { useState, useEffect } from 'react';

interface BoardFormModalProps {
  isOpen: boolean;
  onSubmit: (title: string, contents: string) => Promise<boolean>;
  onClose: () => void;
}

export const BoardFormModal: React.FC<BoardFormModalProps> = ({
  isOpen,
  onSubmit,
  onClose,
}) => {
  const [title, setTitle] = useState('');
  const [contents, setContents] = useState('');
  const [error, setError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  // 모달이 열릴 때 상태 초기화
  useEffect(() => {
    if (isOpen) {
      setTitle('');
      setContents('');
      setError('');
      setIsSubmitting(false);
    }
  }, [isOpen]);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    // 단순 밸리데이션
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
        onClose();
      }
    } catch (err) {
      setError('게시글 등록에 실패했습니다. 서버 상태를 확인해 주세요.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h3 className="modal-title">새 게시물 작성</h3>
          <button className="modal-close" onClick={onClose} aria-label="Close modal">
            &times;
          </button>
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
              marginBottom: '20px',
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
              placeholder="게시글 제목을 입력하세요."
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              disabled={isSubmitting}
              maxLength={100}
            />
          </div>

          <div className="form-group" style={{ marginBottom: 0 }}>
            <label className="form-label" htmlFor="board-contents">내용</label>
            <textarea
              id="board-contents"
              className="form-input form-textarea"
              placeholder="내용을 입력하세요."
              value={contents}
              onChange={(e) => setContents(e.target.value)}
              disabled={isSubmitting}
              maxLength={2000}
            />
          </div>

          <div className="modal-footer">
            <button
              type="button"
              className="btn btn-secondary"
              onClick={onClose}
              disabled={isSubmitting}
            >
              취소
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
    </div>
  );
};
