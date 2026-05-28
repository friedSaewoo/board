export interface MatcherMove {
  ply: number;
  moveNumber: number;
  side: string;
  san?: string | null;
  uci?: string | null;
}

export interface MatcherFeedbackMatch {
  segmentIndex: number;
  text: string;
  matchedPly: number | null;
  matchedMoveNumber: number | null;
  matchedSide: string | null;
  matchedSan: string | null;
  matchedUci: string | null;
  confidence: 'HIGH' | 'MEDIUM' | 'LOW' | 'NONE';
  source: 'AUTO' | 'MANUAL';
}

export function normalizeSan(value: string | undefined | null): string;
export function segmentAiFeedback(aiResponse: string): string[];
export function matchAiFeedback(aiResponse: string, moves: MatcherMove[]): MatcherFeedbackMatch[];
