import React, { useEffect, useMemo, useState } from 'react';
import { Chess } from 'chess.js';
import { normalizeChessReviewDetail, RawChessReviewDetail } from '../../api/chessReviews';
import { ChessMoveAnalysis, ChessReviewDetail, MoveClassification, Toast } from '../../types';
import blackBishopUrl from '../../assets/chess-pieces/black-bishop.svg';
import blackKingUrl from '../../assets/chess-pieces/black-king.svg';
import blackKnightUrl from '../../assets/chess-pieces/black-knight.svg';
import blackPawnUrl from '../../assets/chess-pieces/black-pawn.svg';
import blackQueenUrl from '../../assets/chess-pieces/black-queen.svg';
import blackRookUrl from '../../assets/chess-pieces/black-rook.svg';
import whiteBishopUrl from '../../assets/chess-pieces/white-bishop.svg';
import whiteKingUrl from '../../assets/chess-pieces/white-king.svg';
import whiteKnightUrl from '../../assets/chess-pieces/white-knight.svg';
import whitePawnUrl from '../../assets/chess-pieces/white-pawn.svg';
import whiteQueenUrl from '../../assets/chess-pieces/white-queen.svg';
import whiteRookUrl from '../../assets/chess-pieces/white-rook.svg';

interface ChessReviewDetailViewProps {
  reviewId: number;
  onBack: () => void;
  onToast: (message: string, type: Toast['type']) => void;
  onSessionExpired: () => void;
}

type BoardPiece = { type: string; color: string } | null;
type BoardArrow = {
  kind: 'best';
  x1: number;
  y1: number;
  x2: number;
  y2: number;
};
type PieceAsset = { src: string; alt: string };
type PatternMoment = {
  key: string;
  move: ChessMoveAnalysis;
  label: string;
  description: string;
  tone: 'strong' | 'good' | 'warning' | 'danger' | 'neutral';
};
type PatternSection = {
  key: string;
  title: string;
  description: string;
  moments: PatternMoment[];
};

const pieceAssets: Record<string, PieceAsset> = {
  wp: { src: whitePawnUrl, alt: '백 폰' },
  wn: { src: whiteKnightUrl, alt: '백 나이트' },
  wb: { src: whiteBishopUrl, alt: '백 비숍' },
  wr: { src: whiteRookUrl, alt: '백 룩' },
  wq: { src: whiteQueenUrl, alt: '백 퀸' },
  wk: { src: whiteKingUrl, alt: '백 킹' },
  bp: { src: blackPawnUrl, alt: '흑 폰' },
  bn: { src: blackKnightUrl, alt: '흑 나이트' },
  bb: { src: blackBishopUrl, alt: '흑 비숍' },
  br: { src: blackRookUrl, alt: '흑 룩' },
  bq: { src: blackQueenUrl, alt: '흑 퀸' },
  bk: { src: blackKingUrl, alt: '흑 킹' },
};

const classificationLabel = (classification?: string | null) => {
  const labels: Record<string, string> = {
    EXCELLENT: '탁월',
    BEST: '베스트',
    GOOD: '좋음',
    INACCURACY: '부정확',
    MISTAKE: '실수',
    BLUNDER: '블런더',
  };
  return classification ? labels[classification] || classification : '-';
};

const classificationTone = (classification?: MoveClassification | null) => {
  switch (classification) {
    case 'EXCELLENT':
    case 'BEST':
      return 'strong';
    case 'GOOD':
      return 'good';
    case 'INACCURACY':
      return 'warning';
    case 'MISTAKE':
    case 'BLUNDER':
      return 'danger';
    default:
      return 'neutral';
  }
};

const isCritical = (move?: ChessMoveAnalysis) => classificationTone(move?.classification) === 'danger';

const formatMoveTitle = (move?: ChessMoveAnalysis) => {
  if (!move) return '시작 포지션';
  return `${move.moveNumber}${move.side === 'BLACK' ? '...' : '.'} ${move.san || move.uci || '-'}`;
};

const formatEval = (cp?: number | null, mate?: number | null) => {
  if (typeof mate === 'number') return `M${mate}`;
  if (typeof cp === 'number') return `${cp > 0 ? '+' : ''}${(cp / 100).toFixed(2)}`;
  return '-';
};

const formatLoss = (loss?: number | null) => {
  if (typeof loss !== 'number') return '-';
  return `${loss}cp`;
};

const normalizedMove = (move?: string | null) => (move || '').trim().toLowerCase();

const bestDestination = (move?: ChessMoveAnalysis) => {
  const bestMove = normalizedMove(move?.bestMove);
  return bestMove.length >= 4 ? bestMove.slice(2, 4) : '';
};

const hasDifferentBestMove = (move?: ChessMoveAnalysis) => {
  const bestMove = normalizedMove(move?.bestMove);
  const playedMove = normalizedMove(move?.uci);
  return bestMove.length >= 4 && playedMove.length >= 4 && bestMove !== playedMove;
};

const isMissedBestMove = (move?: ChessMoveAnalysis) => (
  hasDifferentBestMove(move) && typeof move?.centipawnLoss === 'number' && move.centipawnLoss > 35
);

const isHighImpactSwing = (move?: ChessMoveAnalysis) => {
  if (!move) return false;
  if (typeof move.centipawnLoss === 'number' && move.centipawnLoss >= 120) return true;
  if (typeof move.scoreBeforeMate === 'number' || typeof move.scoreAfterMate === 'number') return true;
  if (typeof move.scoreBeforeCp === 'number' && typeof move.scoreAfterCp === 'number') {
    return Math.abs(move.scoreAfterCp - move.scoreBeforeCp) >= 180;
  }
  return false;
};

const phaseLabel = (move?: ChessMoveAnalysis) => {
  if (!move) return '시작';
  if (move.moveNumber <= 8) return '오프닝';
  if (move.moveNumber <= 30) return '미들게임';
  return '엔드게임';
};

const createMoment = (
  key: string,
  move: ChessMoveAnalysis,
  label: string,
  description: string,
  tone: PatternMoment['tone'],
): PatternMoment => ({ key, move, label, description, tone });

const dedupeMoments = (moments: PatternMoment[]) => {
  const seen = new Set<string>();
  return moments.filter((moment) => {
    const key = `${moment.key}:${moment.move.ply}`;
    if (seen.has(key)) return false;
    seen.add(key);
    return true;
  }).sort((a, b) => a.move.ply - b.move.ply);
};

const buildPatternSections = (
  moves: ChessMoveAnalysis[],
  feedbackCountByPly: Map<number, number>,
  playerColor?: string,
): PatternSection[] => {
  const studiedMoves = playerColor
    ? moves.filter((move) => move.side === playerColor)
    : moves;
  const destinationCounts = studiedMoves.reduce((counts, move) => {
    if (!isMissedBestMove(move)) return counts;
    const destination = bestDestination(move);
    if (destination) counts.set(destination, (counts.get(destination) || 0) + 1);
    return counts;
  }, new Map<string, number>());

  const missedBest = studiedMoves
    .filter(isMissedBestMove)
    .map((move) => createMoment(
      'missed-best',
      move,
      '놓친 최선수',
      `실전 ${move.uci || move.san || '-'} 대신 엔진은 ${move.bestMove || '-'}를 추천합니다.`,
      classificationTone(move.classification) === 'danger' ? 'danger' : 'warning',
    ));
  const repeatedDestinations = studiedMoves
    .filter((move) => isMissedBestMove(move) && (destinationCounts.get(bestDestination(move)) || 0) >= 2)
    .map((move) => createMoment(
      'repeated-best-destination',
      move,
      `반복 목적지 ${bestDestination(move)}`,
      `여러 장면에서 엔진 최선수가 ${bestDestination(move)} 칸을 노립니다. 같은 목적지를 반복 학습하세요.`,
      'strong',
    ));
  const highImpact = studiedMoves
    .filter(isHighImpactSwing)
    .map((move) => createMoment(
      'high-impact-swing',
      move,
      '평가 급변',
      `이 수 전후 평가 변화가 큽니다. 손실 ${formatLoss(move.centipawnLoss)}와 추천수 ${move.bestMove || '-'}를 비교하세요.`,
      'danger',
    ));
  const aiLinked = studiedMoves
    .filter((move) => (feedbackCountByPly.get(move.ply) || 0) > 0)
    .map((move) => createMoment(
      'ai-linked',
      move,
      'AI 해설 연결',
      '외부 AI 피드백이 연결된 복습 장면입니다.',
      'good',
    ));
  const openingPrinciples = studiedMoves
    .filter((move) => move.moveNumber <= 8 && isMissedBestMove(move))
    .map((move) => createMoment(
      'opening-principle',
      move,
      '오프닝 원칙',
      `초반 정석/원칙에서 ${move.bestMove || '-'}가 더 나은 후보였습니다.`,
      'warning',
    ));
  const endgameConversion = studiedMoves
    .filter((move) => phaseLabel(move) === '엔드게임' && (isMissedBestMove(move) || isHighImpactSwing(move)))
    .map((move) => createMoment(
      'endgame-conversion',
      move,
      '엔드게임 전환',
      `후반 계산/전환 장면입니다. 실전수와 ${move.bestMove || '-'}를 비교하세요.`,
      'strong',
    ));

  return [
    {
      key: 'missed-best',
      title: '놓친 최선수',
      description: '내 수와 엔진 추천이 의미 있게 갈린 장면',
      moments: dedupeMoments(missedBest),
    },
    {
      key: 'repeated-best-destination',
      title: '반복 패턴',
      description: '같은 최선수 목적지가 반복되는 장면',
      moments: dedupeMoments(repeatedDestinations),
    },
    {
      key: 'high-impact-swing',
      title: '평가 급변',
      description: '손실/평가 변화가 커서 복습 가치가 높은 장면',
      moments: dedupeMoments(highImpact),
    },
    {
      key: 'phase-signals',
      title: '국면별 학습',
      description: '오프닝 원칙과 엔드게임 전환 장면',
      moments: dedupeMoments([...openingPrinciples, ...endgameConversion]),
    },
    {
      key: 'ai-linked',
      title: 'AI 해설 연결',
      description: '붙여넣은 AI 피드백과 자동 연결된 장면',
      moments: dedupeMoments(aiLinked),
    },
  ].filter((section) => section.moments.length > 0);
};

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

const boardPointForSquare = (square: string | undefined, rows: number[], cols: number[]) => {
  if (!square || square.length < 2) return null;
  const file = square.charCodeAt(0) - 97;
  const rank = Number(square[1]);
  const row = 8 - rank;
  const col = file;
  const rowIndex = rows.indexOf(row);
  const colIndex = cols.indexOf(col);
  if (rowIndex < 0 || colIndex < 0) return null;
  return {
    x: ((colIndex + 0.5) / 8) * 100,
    y: ((rowIndex + 0.5) / 8) * 100,
  };
};

const buildBoardArrow = (
  kind: BoardArrow['kind'],
  from: string | undefined,
  to: string | undefined,
  rows: number[],
  cols: number[],
): BoardArrow | null => {
  const start = boardPointForSquare(from, rows, cols);
  const end = boardPointForSquare(to, rows, cols);
  if (!start || !end) return null;

  const dx = end.x - start.x;
  const dy = end.y - start.y;
  const length = Math.hypot(dx, dy);
  const shorten = Math.min(4.2, length * 0.28);

  return {
    kind,
    x1: start.x,
    y1: start.y,
    x2: length ? end.x - (dx / length) * shorten : end.x,
    y2: length ? end.y - (dy / length) * shorten : end.y,
  };
};

export const ChessReviewDetailView: React.FC<ChessReviewDetailViewProps> = ({ reviewId, onBack, onToast, onSessionExpired }) => {
  const [review, setReview] = useState<ChessReviewDetail | null>(null);
  const [currentPly, setCurrentPly] = useState(0);
  const [isLoading, setIsLoading] = useState(true);
  const [isFlipped, setIsFlipped] = useState(false);

  useEffect(() => {
    void fetchReview();
  }, [reviewId]);

  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      const target = event.target as HTMLElement | null;
      const tagName = target?.tagName;
      if (tagName === 'INPUT' || tagName === 'TEXTAREA' || tagName === 'SELECT' || target?.isContentEditable) {
        return;
      }

      if (event.key === 'ArrowLeft' || event.key === '<') {
        event.preventDefault();
        setCurrentPly((prev) => Math.max(0, prev - 1));
      }
      if (event.key === 'ArrowRight' || event.key === '>') {
        event.preventDefault();
        setCurrentPly((prev) => Math.min(review?.moves.length ?? 0, prev + 1));
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [review?.moves.length]);

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
      setCurrentPly(0);
    } catch (err) {
      console.error(err);
      onToast(err instanceof Error ? err.message : '체스 리뷰 조회 중 오류가 발생했습니다.', 'error');
    } finally {
      setIsLoading(false);
    }
  };

  const board = useMemo(() => buildPosition(review?.moves || [], currentPly), [review?.moves, currentPly]);
  const moveRows = useMemo(() => {
    const rows = new Map<number, { moveNumber: number; white?: ChessMoveAnalysis; black?: ChessMoveAnalysis }>();
    for (const move of review?.moves || []) {
      const row = rows.get(move.moveNumber) || { moveNumber: move.moveNumber };
      if (move.side === 'WHITE') {
        row.white = move;
      } else if (move.side === 'BLACK') {
        row.black = move;
      }
      rows.set(move.moveNumber, row);
    }
    return Array.from(rows.values()).sort((a, b) => a.moveNumber - b.moveNumber);
  }, [review?.moves]);

  const feedbackMatches = review?.feedbackMatches || [];
  const feedbackCountByPly = useMemo(() => {
    const counts = new Map<number, number>();
    for (const match of feedbackMatches) {
      if (typeof match.matchedPly === 'number') {
        counts.set(match.matchedPly, (counts.get(match.matchedPly) || 0) + 1);
      }
    }
    return counts;
  }, [feedbackMatches]);

  const currentMove = currentPly > 0 ? review?.moves.find((move) => move.ply === currentPly) : undefined;
  const currentFeedback = feedbackMatches.filter((match) => match.matchedPly === currentPly);
  const unmatchedFeedback = feedbackMatches.filter((match) => match.matchedPly == null);
  const patternSections = useMemo(
    () => buildPatternSections(review?.moves || [], feedbackCountByPly, review?.playerColor),
    [review?.moves, feedbackCountByPly, review?.playerColor],
  );
  const patternMomentCount = patternSections.reduce((total, section) => total + section.moments.length, 0);
  const actualFrom = currentMove?.uci?.slice(0, 2);
  const actualTo = currentMove?.uci?.slice(2, 4);
  const firstMove = review?.moves[0];
  const nextMove = review?.moves.find((move) => move.ply === currentPly + 1);
  const engineBestMove = currentPly === 0 ? firstMove?.bestMove : nextMove?.bestMove;
  const bestFrom = engineBestMove?.slice(0, 2);
  const bestTo = engineBestMove?.slice(2, 4);
  const blackOrientation = Boolean(review?.playerColor === 'BLACK') !== isFlipped;
  const rows = blackOrientation ? [7, 6, 5, 4, 3, 2, 1, 0] : [0, 1, 2, 3, 4, 5, 6, 7];
  const cols = blackOrientation ? [7, 6, 5, 4, 3, 2, 1, 0] : [0, 1, 2, 3, 4, 5, 6, 7];
  const boardArrows = [
    buildBoardArrow('best', bestFrom, bestTo, rows, cols),
  ].filter((arrow): arrow is BoardArrow => Boolean(arrow));
  const currentPatternMoments = patternSections
    .flatMap((section) => section.moments)
    .filter((moment) => moment.move.ply === currentPly);

  const renderMoveButton = (move?: ChessMoveAnalysis) => {
    if (!move) return <span className="chess-move-cell empty">—</span>;
    const feedbackCount = feedbackCountByPly.get(move.ply) || 0;
    const tone = classificationTone(move.classification);
    return (
      <button
        className={`chess-move-cell tone-${tone} ${currentPly === move.ply ? 'active' : ''}`}
        type="button"
        onClick={() => setCurrentPly(move.ply)}
        title={`${formatMoveTitle(move)} (${move.uci || '-'}) · ${classificationLabel(move.classification)}`}
      >
        <span className="move-cell-main">{move.san || move.uci || '-'}</span>
        <span className="move-cell-meta">
          <span>{classificationLabel(move.classification)}</span>
          {feedbackCount > 0 && <span>AI {feedbackCount}</span>}
        </span>
      </button>
    );
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
      <section className="section-card chess-review-hero">
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

      <div className="chess-learning-grid">
        <section className="section-card chess-board-card">
          <div className={`chess-board ${isCritical(currentMove) ? 'critical' : ''}`}>
            {rows.map((row) => cols.map((col) => {
              const square = squareName(row, col);
              const piece = board[row]?.[col] || null;
              const pieceAsset = piece ? pieceAssets[`${piece.color}${piece.type}`] : undefined;
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
                  <span className={`piece ${piece?.color === 'w' ? 'white-piece' : piece?.color === 'b' ? 'black-piece' : ''}`}>
                    {pieceAsset && <img className="piece-image" src={pieceAsset.src} alt={pieceAsset.alt} />}
                  </span>
                </div>
              );
            }))}
            {boardArrows.length > 0 && (
              <svg className="chess-arrow-layer" viewBox="0 0 100 100" aria-hidden="true">
                <defs>
                  <marker id="best-arrow-head" markerHeight="3.2" markerWidth="3.2" orient="auto" refX="2.7" refY="1.6">
                    <path d="M0,0 L3.2,1.6 L0,3.2 Z" />
                  </marker>
                </defs>
                {boardArrows.map((arrow) => (
                  <line
                    className={`chess-board-arrow ${arrow.kind}-arrow`}
                    key={arrow.kind}
                    markerEnd={`url(#${arrow.kind}-arrow-head)`}
                    x1={arrow.x1}
                    x2={arrow.x2}
                    y1={arrow.y1}
                    y2={arrow.y2}
                  />
                ))}
              </svg>
            )}
          </div>
          <div className="chess-board-controls compact">
            <button className="btn btn-secondary" onClick={() => setCurrentPly(0)} disabled={currentPly === 0}>처음</button>
            <button className="btn btn-secondary" onClick={() => setCurrentPly((prev) => Math.max(0, prev - 1))} disabled={currentPly === 0}>이전</button>
            <span className="chess-ply-counter">{currentPly} / {review.moves.length}</span>
            <button className="btn btn-secondary" onClick={() => setCurrentPly((prev) => Math.min(review.moves.length, prev + 1))} disabled={currentPly >= review.moves.length}>다음</button>
            <button className="btn btn-secondary" onClick={() => setCurrentPly(review.moves.length)} disabled={currentPly >= review.moves.length}>마지막</button>
          </div>
          <div className="chess-board-legend">
            <span><i className="legend-dot actual" />방금 둔 수</span>
            <span><i className="legend-dot best" />엔진 다음 최선수</span>
            <span>←/→ 키로 이동</span>
          </div>
        </section>

        <section className="section-card chess-side-panel">
          <div className="chess-panel-heading">
            <div>
              <h3 className="section-title">수 목록</h3>
              <p className="chess-helper-text">중요 수를 목차처럼 찍고, 아래 전체 수순에서 바로 이동합니다.</p>
            </div>
            <span className="chess-pill">{currentPly} / {review.moves.length}</span>
          </div>

          <div className="important-move-strip pattern-nav-strip">
            <div className="pattern-nav-title">
              <strong>패턴 목차</strong>
              <span className="chess-pill">{patternMomentCount}개</span>
            </div>
            {patternSections.length === 0 ? (
              <span className="chess-helper-text">자동 표시할 패턴 장면이 없습니다.</span>
            ) : (
              <div className="pattern-section-list">
                {patternSections.map((section) => (
                  <section className="pattern-section" key={section.key}>
                    <div className="pattern-section-heading">
                      <strong>{section.title}</strong>
                      <small>{section.description}</small>
                    </div>
                    <div className="important-move-list">
                      {section.moments.map((moment) => (
                        <button
                          key={`${moment.key}-${moment.move.ply}`}
                          className={`important-move-chip tone-${moment.tone} ${currentPly === moment.move.ply ? 'active' : ''}`}
                          type="button"
                          onClick={() => setCurrentPly(moment.move.ply)}
                          title={moment.description}
                        >
                          <span>{formatMoveTitle(moment.move)}</span>
                          <small>{moment.label}</small>
                        </button>
                      ))}
                    </div>
                  </section>
                ))}
              </div>
            )}
          </div>

          <button
            className={`chess-move-start chess-move-start-fixed ${currentPly === 0 ? 'active' : ''}`}
            type="button"
            onClick={() => setCurrentPly(0)}
          >
            시작 포지션
          </button>

          <div className="chess-move-list">
            {moveRows.map((row) => (
              <div className="chess-move-row" key={row.moveNumber}>
                <span className="chess-move-number">{row.moveNumber}.</span>
                {renderMoveButton(row.white)}
                {renderMoveButton(row.black)}
              </div>
            ))}
          </div>
        </section>

        <section className="section-card chess-feedback-panel">
          <div className="chess-panel-heading">
            <div>
              <h3 className="section-title">현재 수 해설</h3>
              <p className="chess-helper-text">보드와 수 목록을 움직이면 이 영역만 바뀝니다.</p>
            </div>
            <span className={`chess-pill tone-${classificationTone(currentMove?.classification)}`}>
              {currentMove ? classificationLabel(currentMove.classification) : '시작'}
            </span>
          </div>

          <article className="current-move-study-card">
            <div className="study-card-title">
              <span>{formatMoveTitle(currentMove)}</span>
              <small>{phaseLabel(currentMove)}</small>
            </div>
            {currentMove ? (
              <>
                <dl className="study-metrics-grid">
                  <div>
                    <dt>실전 수</dt>
                    <dd>{currentMove.uci || '-'}</dd>
                  </div>
                  <div>
                    <dt>대안 추천</dt>
                    <dd>{currentMove.bestMove || '-'}</dd>
                  </div>
                  <div>
                    <dt>다음 최선수</dt>
                    <dd>{engineBestMove || '-'}</dd>
                  </div>
                  <div>
                    <dt>손실</dt>
                    <dd>{formatLoss(currentMove.centipawnLoss)}</dd>
                  </div>
                  <div>
                    <dt>평가</dt>
                    <dd>{formatEval(currentMove.scoreBeforeCp, currentMove.scoreBeforeMate)} → {formatEval(currentMove.scoreAfterCp, currentMove.scoreAfterMate)}</dd>
                  </div>
                </dl>
                <div className="study-line-box">
                  <strong>Stockfish PV</strong>
                  <span>{currentMove.principalVariation?.length ? currentMove.principalVariation.join(' ') : '추천 라인이 없습니다.'}</span>
                </div>
                <div className="study-pattern-box">
                  <strong>학습 패턴</strong>
                  {currentPatternMoments.length === 0 ? (
                    <span className="chess-helper-text">이 수에는 자동 패턴 태그가 없습니다.</span>
                  ) : (
                    <div className="study-pattern-list">
                      {currentPatternMoments.map((moment) => (
                        <span className={`study-pattern-chip tone-${moment.tone}`} key={`${moment.key}-${moment.move.ply}`}>
                          <b>{moment.label}</b>
                          <small>{moment.description}</small>
                        </span>
                      ))}
                    </div>
                  )}
                </div>
              </>
            ) : (
              <p className="chess-helper-text">수 목록에서 궁금한 수를 누르거나 방향키로 이동하세요.</p>
            )}
          </article>

          <div className="current-feedback-block">
            <div className="feedback-block-title">
              <strong>AI 피드백</strong>
              <span className="chess-pill">{currentFeedback.length}개</span>
            </div>
            {feedbackMatches.length === 0 ? (
              <p className="chess-helper-text">AI 응답 세그먼트가 없습니다.</p>
            ) : currentFeedback.length === 0 ? (
              <div className="feedback-empty-state">
                <strong>{currentMove ? '이 수에 연결된 피드백이 없습니다.' : '시작 포지션입니다.'}</strong>
                <span>중요 수 목차나 수 목록에서 다른 수를 선택하면 연결된 해설만 바로 표시됩니다.</span>
              </div>
            ) : (
              <article className="feedback-card active current-feedback-combined">
                <div className="combined-feedback-body">
                  {currentFeedback.map((match) => (
                    <section key={match.segmentIndex} className="combined-feedback-item">
                      <span className="combined-feedback-index">#{match.segmentIndex + 1}</span>
                      <p>{match.text}</p>
                    </section>
                  ))}
                </div>
              </article>
            )}
          </div>
        </section>
      </div>

      {unmatchedFeedback.length > 0 && (
        <section className="section-card unmatched-feedback-section">
          <div className="section-header">
            <div>
              <h3 className="section-title">미매칭 AI 피드백</h3>
              <p className="chess-helper-text">
                특정 수와 자동 연결되지 않은 피드백입니다. 복잡한 직접 매칭 없이 참고용 카드로만 모아둡니다.
              </p>
            </div>
            <span className="chess-pill">{unmatchedFeedback.length}개</span>
          </div>
          <div className="unmatched-feedback-grid">
            {unmatchedFeedback.map((match) => (
              <article key={match.segmentIndex} className="feedback-card compact unmatched">
                <div className="feedback-card-header">
                  <strong>#{match.segmentIndex + 1}</strong>
                  <span className="chess-pill">{match.confidence} · {match.source}</span>
                </div>
                <p>{match.text}</p>
              </article>
            ))}
          </div>
        </section>
      )}
    </div>
  );
};
