export interface Board {
  boardId: number;
  title: string;
  contents: string;
  createdAt: string;
  updatedAt: string;
}

export interface PageInfo {
  pageNum: number;
  pageSize: number;
  totalElement: number;
  totalPage: number;
  blockStart: number;
  blockEnd: number;
  hasPrev: boolean;
  hasNext: boolean;
}

export interface PagedResult<T = Board> {
  content: T[];
  pageInfo: PageInfo;
}

export interface Toast {
  id: number;
  message: string;
  type: 'success' | 'error' | 'info';
}

export interface Member {
  id: number;
  name: string;
  email: string;
  role: 'ROLE_USER' | 'ROLE_ADMIN';
}

export type PlayerColor = 'WHITE' | 'BLACK';

export type MoveClassification = 'EXCELLENT' | 'BEST' | 'GOOD' | 'INACCURACY' | 'MISTAKE' | 'BLUNDER' | string;

export interface ChessAnalysisRequest {
  pgn: string;
  playerColor: PlayerColor;
}

export interface ChessGameMetadata {
  [key: string]: string | number | null | undefined;
}

export interface ChessAnalysisSummary {
  averageCentipawnLoss?: number | null;
  inaccuracies?: number | null;
  mistakes?: number | null;
  blunders?: number | null;
  biggestSwingPly?: number | null;
  headline?: string;
}

export interface ChessMoveAnalysis {
  ply: number;
  moveNumber: number;
  side: PlayerColor | string;
  san?: string;
  uci?: string;
  scoreBeforeCp?: number | null;
  scoreAfterCp?: number | null;
  scoreBeforeMate?: number | null;
  scoreAfterMate?: number | null;
  centipawnLoss?: number | null;
  classification?: MoveClassification;
  bestMove?: string;
  principalVariation?: string[];
}

export interface ChessAnalysisResponse {
  analysisId: string;
  metadata: ChessGameMetadata;
  playerColor: PlayerColor;
  moveCount: number;
  summary: ChessAnalysisSummary;
  moves: ChessMoveAnalysis[];
  aiPrompt: string;
}

export type FeedbackMatchConfidence = 'HIGH' | 'MEDIUM' | 'LOW' | 'NONE';
export type FeedbackMatchSource = 'AUTO' | 'MANUAL';

export interface FeedbackMatch {
  segmentIndex: number;
  text: string;
  matchedPly: number | null;
  matchedMoveNumber?: number | null;
  matchedSide?: PlayerColor | string | null;
  matchedSan?: string | null;
  matchedUci?: string | null;
  confidence: FeedbackMatchConfidence;
  source: FeedbackMatchSource;
}

export interface ChessReviewSummary {
  id: number;
  reviewId?: number;
  title: string;
  whiteName?: string | null;
  blackName?: string | null;
  result?: string | null;
  playerColor: PlayerColor;
  moveCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface ChessReviewDetail extends ChessReviewSummary {
  originalPgn: string;
  metadata: ChessGameMetadata;
  summary: ChessAnalysisSummary;
  moves: ChessMoveAnalysis[];
  aiPrompt: string;
  aiResponse: string;
  feedbackMatches: FeedbackMatch[];
  fenSnapshots?: string[];
}

export interface ChessReviewCreateRequest {
  analysisId: string;
  aiResponse: string;
}

export type ActiveMenu = 'dashboard' | 'board' | 'chess' | 'chessReviews' | 'settings' | 'auth';
