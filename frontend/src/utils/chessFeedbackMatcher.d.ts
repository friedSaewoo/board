import type { ChessMoveAnalysis, FeedbackMatch } from '../types';

export function normalizeSan(value: string | undefined | null): string;
export function segmentAiFeedback(aiResponse: string): string[];
export function matchAiFeedback(aiResponse: string, moves: ChessMoveAnalysis[]): FeedbackMatch[];
