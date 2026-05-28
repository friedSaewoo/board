import React, { useState } from 'react';
import { createChessReview } from '../api/chessReviews';
import { ChessAnalysisResponse, ChessReviewSummary, PlayerColor, Toast } from '../types';

interface ChessAnalysisViewProps {
  onToast: (message: string, type: Toast['type']) => void;
  onSessionExpired: () => void;
  onReviewCreated: (review: ChessReviewSummary) => void;
}

type ErrorBody = {
  message?: string;
  errors?: Array<{ field?: string; reason?: string }>;
};

const sideLabel = (side: PlayerColor | string | null | undefined) => (side === 'WHITE' ? '백' : side === 'BLACK' ? '흑' : side || '-');

const formatNumber = (value?: number | null) => {
  if (typeof value !== 'number') return '-';
  return Number.isInteger(value) ? value.toString() : value.toFixed(1);
};

const formatScore = (cp?: number | null, mate?: number | null) => {
  if (typeof mate === 'number') return `M${mate > 0 ? '+' : ''}${mate}`;
  if (typeof cp === 'number') return `${cp > 0 ? '+' : ''}${cp}`;
  return '-';
};

const metadataEntries = (metadata: ChessAnalysisResponse['metadata']) => Object.entries(metadata || {})
  .filter(([, value]) => value !== undefined && value !== null && String(value).trim().length > 0);

const isAuthFailure = (response: Response) => response.status === 401 || response.status === 403;

export const ChessAnalysisView: React.FC<ChessAnalysisViewProps> = ({ onToast, onSessionExpired, onReviewCreated }) => {
  const [pgn, setPgn] = useState('');
  const [playerColor, setPlayerColor] = useState<PlayerColor>('WHITE');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isCreatingReview, setIsCreatingReview] = useState(false);
  const [error, setError] = useState('');
  const [result, setResult] = useState<ChessAnalysisResponse | null>(null);
  const [aiResponse, setAiResponse] = useState('');
  const [createdReview, setCreatedReview] = useState<ChessReviewSummary | null>(null);

  const readErrorMessage = async (response: Response, fallback = '체스 분석 요청에 실패했습니다.') => {
    const body = await response.json().catch(() => ({} as ErrorBody)) as ErrorBody;
    const fieldError = body.errors?.find((item) => item.reason)?.reason;
    return fieldError || body.message || fallback;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    const trimmedPgn = pgn.trim();
    if (!trimmedPgn) {
      setError('PGN 텍스트를 입력해 주세요. URL, 이미지, OCR 입력은 지원하지 않습니다.');
      setResult(null);
      setAiResponse('');
      setCreatedReview(null);
      return;
    }

    setError('');
    setAiResponse('');
    setCreatedReview(null);
    setIsSubmitting(true);

    try {
      const response = await fetch('/chess/analyze', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ pgn: trimmedPgn, playerColor }),
        credentials: 'include',
      });

      if (isAuthFailure(response)) {
        onSessionExpired();
        throw new Error('로그인 세션이 만료되었습니다. 다시 로그인해 주세요.');
      }

      const contentType = response.headers.get('content-type') || '';
      if (!contentType.includes('application/json')) {
        if (response.redirected || response.url.includes('/login')) {
          onSessionExpired();
          throw new Error('로그인 세션이 만료되었습니다. 다시 로그인해 주세요.');
        }
        throw new Error('서버가 JSON 분석 결과를 반환하지 않았습니다. 잠시 후 다시 시도해 주세요.');
      }

      if (!response.ok) {
        throw new Error(await readErrorMessage(response));
      }

      const data = await response.json() as ChessAnalysisResponse;
      setResult(data);
      onToast('체스 분석이 완료되었습니다. 프롬프트를 외부 AI에 붙여넣고 응답을 다시 입력해 주세요.', 'success');
    } catch (err) {
      const message = err instanceof Error ? err.message : '체스 분석 요청 중 오류가 발생했습니다.';
      setError(message);
      setResult(null);
      onToast(message, 'error');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleCopyPrompt = async () => {
    if (!result?.aiPrompt) return;

    try {
      if (navigator.clipboard?.writeText) {
        await navigator.clipboard.writeText(result.aiPrompt);
      } else {
        const textarea = document.createElement('textarea');
        textarea.value = result.aiPrompt;
        textarea.style.position = 'fixed';
        textarea.style.opacity = '0';
        document.body.appendChild(textarea);
        textarea.focus();
        textarea.select();
        document.execCommand('copy');
        document.body.removeChild(textarea);
      }
      onToast('한국어 AI 코칭 프롬프트를 복사했습니다.', 'success');
    } catch (err) {
      console.error(err);
      onToast('프롬프트 복사에 실패했습니다. 직접 선택해 복사해 주세요.', 'error');
    }
  };

  const handleCreateReview = async () => {
    const analysisId = result?.analysisId?.trim();
    const trimmedAiResponse = aiResponse.trim();

    if (!result || !analysisId || !trimmedAiResponse) {
      return;
    }

    setIsCreatingReview(true);
    setError('');

    try {
      const review = await createChessReview({ analysisId, aiResponse: trimmedAiResponse }) as ChessReviewSummary;
      setCreatedReview(review);
      onToast('체스 리뷰가 전용 게시판에 생성되었습니다.', 'success');
      onReviewCreated(review);
    } catch (err) {
      const message = err instanceof Error ? err.message : '체스 리뷰 생성 중 오류가 발생했습니다.';
      setError(message);
      onToast(message, 'error');
    } finally {
      setIsCreatingReview(false);
    }
  };

  const handleReviewCreateKeyDown = async (event: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if ((event.metaKey || event.ctrlKey) && event.key === 'Enter') {
      event.preventDefault();
      await handleCreateReview();
    }
  };

  const entries = result ? metadataEntries(result.metadata) : [];
  const canCreateReview = Boolean(result?.analysisId?.trim()) && aiResponse.trim().length > 0 && !isCreatingReview;

  return (
    <div className="chess-analysis-layout">
      <section className="section-card chess-input-card">
        <div className="section-header">
          <div>
            <h2 className="section-title">체스 PGN 분석</h2>
            <p className="chess-helper-text">
              PGN을 붙여넣고 플레이한 색을 선택하면 Stockfish 분석 결과와 외부 AI에 붙여넣을 한국어 코칭 프롬프트를 생성합니다.
            </p>
          </div>
        </div>

        <form onSubmit={handleSubmit}>
          {error && <div className="form-error">⚠️ {error}</div>}

          <div className="form-group">
            <label className="form-label" htmlFor="chess-pgn">PGN 텍스트</label>
            <textarea
              id="chess-pgn"
              className="form-input form-textarea chess-pgn-textarea"
              placeholder={'[Event "Casual Game"]\n[White "User"]\n[Black "Opponent"]\n\n1. e4 e5 2. Nf3 Nc6 3. Bb5 a6'}
              value={pgn}
              onChange={(event) => setPgn(event.target.value)}
              disabled={isSubmitting}
            />
            <p className="chess-helper-text">분석 결과는 숨김 초안으로 저장되고, AI 응답을 붙여넣어야 전용 체스 리뷰 게시글이 생성됩니다.</p>
          </div>

          <div className="form-group">
            <span className="form-label">내가 플레이한 색</span>
            <div className="chess-color-options" role="radiogroup" aria-label="플레이어 색 선택">
              {(['WHITE', 'BLACK'] as PlayerColor[]).map((color) => (
                <label key={color} className={`chess-color-option ${playerColor === color ? 'active' : ''}`}>
                  <input
                    type="radio"
                    name="playerColor"
                    value={color}
                    checked={playerColor === color}
                    onChange={() => setPlayerColor(color)}
                    disabled={isSubmitting}
                  />
                  <span>{color === 'WHITE' ? '⚪ 백으로 분석' : '⚫ 흑으로 분석'}</span>
                </label>
              ))}
            </div>
          </div>

          <button className="btn btn-primary" type="submit" disabled={isSubmitting}>
            {isSubmitting ? (
              <>
                <div className="loading-spinner" style={{ width: '14px', height: '14px', borderWidth: '2px' }} />
                분석 중...
              </>
            ) : (
              'Stockfish 분석 요청'
            )}
          </button>
        </form>
      </section>

      {result && (
        <>
          <section className="section-card chess-summary-card">
            <div className="section-header">
              <div>
                <h2 className="section-title">분석 요약</h2>
                <p className="chess-helper-text">선택 색상: {sideLabel(result.playerColor || playerColor)}</p>
              </div>
              <span className="chess-pill">총 {result.moveCount ?? result.moves.length}수</span>
            </div>

            <div className="chess-summary-grid">
              <div className="metric-card chess-metric-card">
                <div className="metric-info">
                  <h3>평균 센티폰 손실</h3>
                  <p>{formatNumber(result.summary.averageCentipawnLoss)}</p>
                </div>
                <div className="metric-icon">♟️</div>
              </div>
              <div className="metric-card chess-metric-card">
                <div className="metric-info">
                  <h3>부정확/실수/블런더</h3>
                  <p>{formatNumber(result.summary.inaccuracies)} / {formatNumber(result.summary.mistakes)} / {formatNumber(result.summary.blunders)}</p>
                </div>
                <div className="metric-icon">📉</div>
              </div>
              <div className="metric-card chess-metric-card">
                <div className="metric-info">
                  <h3>가장 큰 변화 Ply</h3>
                  <p>{formatNumber(result.summary.biggestSwingPly)}</p>
                </div>
                <div className="metric-icon">⚡</div>
              </div>
            </div>

            {result.summary.headline && (
              <div className="chess-headline">{result.summary.headline}</div>
            )}

            {entries.length > 0 && (
              <div className="detail-meta chess-metadata-grid">
                {entries.map(([key, value]) => (
                  <div className="meta-item" key={key}>
                    <span>{key}</span>
                    <p>{String(value)}</p>
                  </div>
                ))}
              </div>
            )}
          </section>

          <section className="section-card">
            <div className="section-header">
              <div>
                <h2 className="section-title">모든 수 분석</h2>
                <p className="chess-helper-text">백엔드가 반환한 모든 ply를 순서대로 표시합니다.</p>
              </div>
            </div>

            <div className="table-container">
              <table className="admin-table chess-move-table">
                <thead>
                  <tr>
                    <th>Ply</th>
                    <th>수</th>
                    <th>진영</th>
                    <th>SAN / UCI</th>
                    <th>전/후 평가</th>
                    <th>손실</th>
                    <th>분류</th>
                    <th>추천 수 / PV</th>
                  </tr>
                </thead>
                <tbody>
                  {result.moves.length === 0 ? (
                    <tr>
                      <td colSpan={8} style={{ textAlign: 'center', color: 'var(--text-muted)', padding: '32px' }}>
                        표시할 수 분석 결과가 없습니다.
                      </td>
                    </tr>
                  ) : (
                    result.moves.map((move) => (
                      <tr key={move.ply}>
                        <td>#{move.ply}</td>
                        <td>{move.moveNumber}</td>
                        <td>{sideLabel(move.side)}</td>
                        <td>
                          <strong>{move.san || '-'}</strong>
                          {move.uci && <small className="chess-subtext">{move.uci}</small>}
                        </td>
                        <td>{formatScore(move.scoreBeforeCp, move.scoreBeforeMate)} → {formatScore(move.scoreAfterCp, move.scoreAfterMate)}</td>
                        <td>{formatNumber(move.centipawnLoss)}</td>
                        <td><span className="chess-pill">{move.classification || '-'}</span></td>
                        <td>
                          <strong>{move.bestMove || '-'}</strong>
                          {move.principalVariation && move.principalVariation.length > 0 && (
                            <small className="chess-subtext">{move.principalVariation.join(' ')}</small>
                          )}
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </section>

          <section className="section-card chess-prompt-card">
            <div className="section-header">
              <div>
                <h2 className="section-title">한국어 AI 코칭 프롬프트</h2>
                <p className="chess-helper-text">앱은 AI API를 호출하지 않습니다. GPT/Gemini에 프롬프트를 붙여넣고, AI 응답을 아래에 붙여넣어 리뷰를 생성하세요.</p>
              </div>
              <button className="btn btn-secondary" type="button" onClick={handleCopyPrompt}>
                📋 프롬프트 복사
              </button>
            </div>
            <textarea
              className="form-input form-textarea chess-prompt-textarea"
              value={result.aiPrompt}
              readOnly
              aria-label="한국어 AI 코칭 프롬프트"
            />
            <div className="form-group chess-ai-response-group">
              <label className="form-label" htmlFor="chess-ai-response">외부 AI 응답</label>
              <textarea
                id="chess-ai-response"
                className="form-input form-textarea chess-ai-response-textarea"
                placeholder="GPT/Gemini 등 외부 AI의 응답을 여기에 붙여넣으세요."
                value={aiResponse}
                onChange={(event) => setAiResponse(event.target.value)}
                disabled={isCreatingReview}
              />
            </div>
            <button className="btn btn-primary" type="button" disabled={!canCreateReview} onClick={handleCreateReview}>
              {isCreatingReview ? '리뷰 생성 중...' : '체스 리뷰 생성'}
            </button>
          </section>

          <section className="section-card chess-review-create-card">
            <div className="section-header">
              <div>
                <h2 className="section-title">체스 리뷰 생성</h2>
                <p className="chess-helper-text">AI 응답은 전용 체스 리뷰 게시판에만 저장되며, 일반 게시판에는 자동 등록되지 않습니다.</p>
              </div>
            </div>

            {!result.analysisId && (
              <div className="form-error">⚠️ 분석 응답에 analysisId가 없어 리뷰를 생성할 수 없습니다. 백엔드 초안 저장 응답을 확인해 주세요.</div>
            )}

            {createdReview && (
              <div className="chess-review-created-banner">
                <div>
                  <strong>체스 리뷰 생성 완료</strong>
                  <p>#{createdReview.id} {createdReview.title}</p>
                </div>
                <button className="btn btn-secondary" type="button" onClick={() => onReviewCreated(createdReview)}>
                  리뷰 열기
                </button>
              </div>
            )}

            <div className="form-group">
              <label className="form-label" htmlFor="ai-response">외부 AI 응답</label>
              <textarea
                id="ai-response"
                className="form-input form-textarea chess-ai-response-textarea"
                value={aiResponse}
                onChange={(event) => setAiResponse(event.target.value)}
                onKeyDown={handleReviewCreateKeyDown}
                placeholder="GPT/Gemini 등 외부 AI가 반환한 한국어 리뷰를 붙여넣으세요. Ctrl/⌘ + Enter로 생성할 수 있습니다."
                disabled={isCreatingReview}
              />
            </div>

            <button className="btn btn-primary" type="button" disabled={!canCreateReview} onClick={handleCreateReview}>
              {isCreatingReview ? (
                <>
                  <div className="loading-spinner" style={{ width: '14px', height: '14px', borderWidth: '2px' }} />
                  리뷰 생성 중...
                </>
              ) : (
                '체스 리뷰 생성'
              )}
            </button>
          </section>

          <section className="section-card chess-review-create-card">
            <div className="section-header">
              <div>
                <h2 className="section-title">체스 리뷰 생성</h2>
                <p className="chess-helper-text">AI 응답은 전용 체스 리뷰 게시판에만 저장되며, 일반 게시판에는 자동 등록되지 않습니다.</p>
              </div>
            </div>

            {!result.analysisId && (
              <div className="form-error">⚠️ 분석 응답에 analysisId가 없어 리뷰를 생성할 수 없습니다. 백엔드 초안 저장 응답을 확인해 주세요.</div>
            )}

            {createdReview && (
              <div className="chess-review-created-banner">
                <div>
                  <strong>체스 리뷰 생성 완료</strong>
                  <p>#{createdReview.id} {createdReview.title}</p>
                </div>
                <button className="btn btn-secondary" type="button" onClick={() => onReviewCreated(createdReview)}>
                  리뷰 열기
                </button>
              </div>
            )}

            <div className="form-group">
              <label className="form-label" htmlFor="ai-response">외부 AI 응답</label>
              <textarea
                id="ai-response"
                className="form-input form-textarea chess-ai-response-textarea"
                value={aiResponse}
                onChange={(event) => setAiResponse(event.target.value)}
                onKeyDown={handleReviewCreateKeyDown}
                placeholder="GPT/Gemini 등 외부 AI가 반환한 한국어 리뷰를 붙여넣으세요. Ctrl/⌘ + Enter로 생성할 수 있습니다."
                disabled={isCreatingReview}
              />
            </div>

            <button className="btn btn-primary" type="button" disabled={!canCreateReview} onClick={handleCreateReview}>
              {isCreatingReview ? (
                <>
                  <div className="loading-spinner" style={{ width: '14px', height: '14px', borderWidth: '2px' }} />
                  리뷰 생성 중...
                </>
              ) : (
                '체스 리뷰 생성'
              )}
            </button>
          </section>
        </>
      )}
    </div>
  );
};
