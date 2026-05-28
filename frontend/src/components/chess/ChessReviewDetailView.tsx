import React, { useEffect, useMemo, useState } from 'react';
import { Chess } from 'chess.js';
import { normalizeChessReviewDetail, RawChessReviewDetail } from '../../api/chessReviews';
import { ChessMoveAnalysis, ChessReviewDetail, FeedbackMatch, Toast } from '../../types';

interface ChessReviewDetailViewProps {
  reviewId: number;
  onBack: () => void;
  onToast: (message: string, type: Toast['type']) => void;
  onSessionExpired: () => void;
}

type BoardPiece = { type: string; color: string } | null;

const pieceMap: Record<string, string> = {
  wp: '♙', wn: '♘', wb: '♗', wr: '♖', wq: '♕', wk: '♔',
  bp: '♟', bn: '♞', bb: '♝', br: '♜', bq: '♛', bk: '♚',
};

const sideLabel = (side?: string | null) => (side === 'WHITE' ? '백' : side === 'BLACK' ? '흑' : '-');
const isCritical = (move?: ChessMoveAnalysis) => move?.classification === 'MISTAKE' || move?.classification === 'BLUNDER';

const squareName = (row: number, col: number) => `${String.fromCharCode(97 + col)}${8 - row}`;

const applyUci = (chess: Chess, uci?: string | null) => {
  if (!uci || uci.length < 4) return;
  try {
    chess.move({ from: uci.slice(0, 2), to: uci.slice(2, 4), promotion: uci[4] });
  } catch {
    // Saved review rendering should be resilient to any legacy/bad UCI row.
  }
};

const buildPosition = (moves: ChessMoveAnalysis[], ply: number) => {
  const chess = new Chess();
  for (const move of moves.slice(0, ply)) {
    applyUci(chess, move.uci);
  }
  return chess.board() as BoardPiece[][];
};

export const ChessReviewDetailView: React.FC<ChessReviewDetailViewProps> = ({ reviewId, onBack, onToast, onSessionExpired }) => {
  const [review, setReview] = useState<ChessReviewDetail | null>(null);
  const [editableMatches, setEditableMatches] = useState<FeedbackMatch[]>([]);
  const [currentPly, setCurrentPly] = useState(0);
  const [isLoading, setIsLoading] = useState(true);
  const [isSavingMatches, setIsSavingMatches] = useState(false);
  const [isFlipped, setIsFlipped] = useState(false);

  useEffect(() => {
    void fetchReview();
  }, [reviewId]);

  const fetchReview = async () => {
    setIsLoading(true);
    try {
      const response = await fetch(`/chess/reviews/${reviewId}`, { credentials: 'include' });
      if (response.status === 401 || response.status === 403) {
        onSessionExpired();
        throw new Error('로그인 세션이 만료되었습니다.');
      }
      if (!response.ok) throw new Error('체스 리뷰를 불러오지 못했습니다.');
      const data = normalizeChessReviewDetail(await response.json() as RawChessReviewDetail);
      setReview(data);
      setEditableMatches(data.feedbackMatches || []);
      setCurrentPly(0);
    } catch (err) {
      console.error(err);
      onToast(err instanceof Error ? err.message : '체스 리뷰 조회 중 오류가 발생했습니다.', 'error');
    } finally {
      setIsLoading(false);
    }
  };

  const board = useMemo(() => buildPosition(review?.moves || [], currentPly), [review?.moves, currentPly]);
  const currentMove = currentPly > 0 ? review?.moves.find((move) => move.ply === currentPly) : undefined;
  const currentFeedback = editableMatches.filter((match) => match.matchedPly === currentPly);
  const actualFrom = currentMove?.uci?.slice(0, 2);
  const actualTo = currentMove?.uci?.slice(2, 4);
  const bestFrom = currentMove?.bestMove?.slice(0, 2);
  const bestTo = currentMove?.bestMove?.slice(2, 4);
  const blackOrientation = Boolean(review?.playerColor === 'BLACK') !== isFlipped;
  const rows = blackOrientation ? [7, 6, 5, 4, 3, 2, 1, 0] : [0, 1, 2, 3, 4, 5, 6, 7];
  const cols = blackOrientation ? [7, 6, 5, 4, 3, 2, 1, 0] : [0, 1, 2, 3, 4, 5, 6, 7];

  const saveMatches = async () => {
    if (!review) return;
    setIsSavingMatches(true);
    try {
      const response = await fetch(`/chess/reviews/${review.id}/matches`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          matches: editableMatches.map((match) => ({
            segmentIndex: match.segmentIndex,
            text: match.text,
            matchedPly: match.matchedPly,
            confidence: match.matchedPly ? match.confidence === 'NONE' ? 'HIGH' : match.confidence : 'NONE',
            source: 'MANUAL',
          })),
        }),
        credentials: 'include',
      });
      if (response.status === 401 || response.status === 403) {
        onSessionExpired();
        throw new Error('로그인 세션이 만료되었습니다.');
      }
      if (!response.ok) throw new Error('매칭 저장에 실패했습니다.');
      const updated = normalizeChessReviewDetail(await response.json() as RawChessReviewDetail);
      setReview(updated);
      setEditableMatches(updated.feedbackMatches || []);
      onToast('AI 피드백 매칭을 저장했습니다.', 'success');
    } catch (err) {
      console.error(err);
      onToast(err instanceof Error ? err.message : '매칭 저장 중 오류가 발생했습니다.', 'error');
    } finally {
      setIsSavingMatches(false);
    }
  };

  const updateMatchPly = (segmentIndex: number, matchedPly: number | null) => {
    setEditableMatches((prev) => prev.map((match) => {
      if (match.segmentIndex !== segmentIndex) return match;
      const move = review?.moves.find((item) => item.ply === matchedPly);
      return {
        ...match,
        matchedPly,
        matchedMoveNumber: move?.moveNumber ?? null,
        matchedSide: move?.side ?? null,
        matchedSan: move?.san ?? null,
        matchedUci: move?.uci ?? null,
        confidence: matchedPly ? 'HIGH' : 'NONE',
        source: 'MANUAL',
      };
    }));
    if (matchedPly) setCurrentPly(matchedPly);
  };

  if (isLoading) {
    return <div className="section-card">체스 리뷰를 불러오는 중...</div>;
  }

  if (!review) {
    return (
      <div className="section-card">
        <p>체스 리뷰를 표시할 수 없습니다.</p>
        <button className="btn btn-secondary" type="button" onClick={onBack}>목록으로</button>
      </div>
    );
  }

  return (
    <div className="chess-review-detail">
      <section className="section-card">
        <div className="section-header">
          <div>
            <h2 className="section-title">{review.title}</h2>
            <p className="chess-helper-text">
              {review.whiteName || 'White'} vs {review.blackName || 'Black'} · 결과 {review.result || '-'} · {review.moveCount}수
            </p>
          </div>
          <div className="header-actions">
            <button className="btn btn-secondary" type="button" onClick={() => setIsFlipped((prev) => !prev)}>보드 뒤집기</button>
            <button className="btn btn-secondary" type="button" onClick={onBack}>목록으로</button>
          </div>
        </div>
      </section>

      <div className="chess-review-grid">
        <section className="section-card chess-board-card">
          <div className={`chess-board ${isCritical(currentMove) ? 'critical' : ''}`}>
            {rows.map((row) => cols.map((col) => {
              const square = squareName(row, col);
              const piece = board[row]?.[col] || null;
              const isLight = (row + col) % 2 === 0;
              const classes = [
                'chess-square',
                isLight ? 'light' : 'dark',
                actualFrom === square ? 'actual-from' : '',
                actualTo === square ? 'actual-to' : '',
                bestFrom === square ? 'best-from' : '',
                bestTo === square ? 'best-to' : '',
              ].filter(Boolean).join(' ');
              return (
                <div className={classes} key={square} title={square}>
                  <span className="square-coordinate">{square}</span>
                  <span className="piece">{piece ? pieceMap[`${piece.color}${piece.type}`] : ''}</span>
                </div>
              );
            }))}
          </div>
          <div className="chess-board-controls">
            <button className="btn btn-secondary" onClick={() => setCurrentPly(0)} disabled={currentPly === 0}>처음</button>
            <button className="btn btn-secondary" onClick={() => setCurrentPly((prev) => Math.max(0, prev - 1))} disabled={currentPly === 0}>이전</button>
            <span>Ply {currentPly} / {review.moves.length}</span>
            <button className="btn btn-secondary" onClick={() => setCurrentPly((prev) => Math.min(review.moves.length, prev + 1))} disabled={currentPly >= review.moves.length}>다음</button>
            <button className="btn btn-secondary" onClick={() => setCurrentPly(review.moves.length)} disabled={currentPly >= review.moves.length}>마지막</button>
          </div>
          <div className="chess-current-move">
            <strong>현재 수:</strong> {currentMove ? `${currentMove.moveNumber}${currentMove.side === 'BLACK' ? '...' : '.'} ${currentMove.san} (${currentMove.uci})` : '시작 포지션'}
            {currentMove && <span className="chess-pill">{currentMove.classification || '-'}</span>}
            <p>실제 수 화살표: {actualFrom && actualTo ? `${actualFrom} → ${actualTo}` : '-'}</p>
            <p>Stockfish 추천/PV: {currentMove?.bestMove || '-'} {currentMove?.principalVariation?.length ? `· ${currentMove.principalVariation.join(' ')}` : ''}</p>
            <p>추천 화살표: {bestFrom && bestTo ? `${bestFrom} → ${bestTo}` : '-'}</p>
          </div>
        </section>

        <section className="section-card chess-side-panel">
          <h3 className="section-title">수 목록</h3>
          <div className="chess-move-list">
            <button className={`chess-move-chip ${currentPly === 0 ? 'active' : ''}`} type="button" onClick={() => setCurrentPly(0)}>시작</button>
            {review.moves.map((move) => (
              <button
                key={move.ply}
                className={`chess-move-chip ${currentPly === move.ply ? 'active' : ''} ${isCritical(move) ? 'critical' : ''}`}
                type="button"
                onClick={() => setCurrentPly(move.ply)}
              >
                {move.moveNumber}{move.side === 'BLACK' ? '...' : '.'} {move.san}
              </button>
            ))}
          </div>
        </section>
      </div>

      <section className="section-card">
        <div className="section-header">
          <div>
            <h2 className="section-title">AI 피드백 매칭</h2>
            <p className="chess-helper-text">세그먼트를 클릭하면 매칭된 수로 이동합니다. 잘못된 매칭은 드롭다운으로 수정 후 저장하세요.</p>
          </div>
          <button className="btn btn-primary" type="button" onClick={() => void saveMatches()} disabled={isSavingMatches}>{isSavingMatches ? '저장 중...' : '매칭 저장'}</button>
        </div>
        <div className="feedback-list">
          {editableMatches.length === 0 ? (
            <p className="chess-helper-text">AI 응답 세그먼트가 없습니다.</p>
          ) : editableMatches.map((match) => (
            <article
              key={match.segmentIndex}
              className={`feedback-card ${match.matchedPly === currentPly ? 'active' : ''}`}
              onClick={() => match.matchedPly && setCurrentPly(match.matchedPly)}
            >
              <div className="feedback-card-header">
                <strong>#{match.segmentIndex + 1}</strong>
                <span className="chess-pill">{match.confidence} · {match.source}</span>
              </div>
              <p>{match.text}</p>
              <label className="form-label" htmlFor={`match-${match.segmentIndex}`}>연결된 수</label>
              <select
                id={`match-${match.segmentIndex}`}
                className="form-input"
                value={match.matchedPly ?? ''}
                onClick={(event) => event.stopPropagation()}
                onChange={(event) => updateMatchPly(match.segmentIndex, event.target.value ? Number(event.target.value) : null)}
              >
                <option value="">미매칭</option>
                {review.moves.map((move) => (
                  <option value={move.ply} key={move.ply}>
                    {move.ply}. {move.moveNumber}{move.side === 'BLACK' ? '...' : '.'} {move.san} ({sideLabel(move.side)})
                  </option>
                ))}
              </select>
            </article>
          ))}
        </div>
        {currentFeedback.length > 0 && (
          <div className="chess-headline">현재 ply에 연결된 AI 피드백 {currentFeedback.length}개가 있습니다.</div>
        )}
      </section>
    </div>
  );
};
