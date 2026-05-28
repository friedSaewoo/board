import { ChessReviewCreateRequest, ChessReviewDetail, ChessReviewSummary, FeedbackMatch, PagedResult } from '../types';

export type ErrorBody = {
  message?: string;
  errors?: Array<{ field?: string; reason?: string }>;
};

export const readApiErrorMessage = async (response: Response, fallback: string) => {
  const body = await response.json().catch(() => ({} as ErrorBody)) as ErrorBody;
  const fieldError = body.errors?.find((item) => item.reason)?.reason;
  return fieldError || body.message || fallback;
};

const normalizeReviewId = <T extends { id?: number; reviewId?: number }>(review: T): T & { id: number } => {
  const id = review.id ?? review.reviewId;
  if (typeof id !== 'number') {
    throw new Error('체스 리뷰 식별자가 응답에 없습니다.');
  }
  return { ...review, id };
};

export type RawChessReviewDetail = Omit<ChessReviewDetail, 'id' | 'feedbackMatches'> & {
  id?: number;
  reviewId?: number;
  feedbackMatches?: FeedbackMatch[];
  matches?: FeedbackMatch[];
};

export const normalizeChessReviewDetail = (review: RawChessReviewDetail): ChessReviewDetail => {
  const normalized = normalizeReviewId(review);
  return {
    ...normalized,
    feedbackMatches: review.feedbackMatches ?? review.matches ?? [],
  };
};

export const fetchChessReviews = async (page: number): Promise<PagedResult<ChessReviewSummary>> => {
  const response = await fetch(`/chess/reviews?pageNum=${page}&pageSize=10&sortBy=id&direction=DESC`, {
    credentials: 'include',
  });

  if (!response.ok) {
    throw new Error(await readApiErrorMessage(response, '체스 리뷰 목록을 불러오지 못했습니다.'));
  }

  const data = await response.json() as PagedResult<ChessReviewSummary>;
  return {
    ...data,
    content: (data.content || []).map(normalizeReviewId),
  };
};

export const createChessReview = async (request: ChessReviewCreateRequest): Promise<ChessReviewDetail | ChessReviewSummary> => {
  const response = await fetch('/chess/reviews', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(request),
    credentials: 'include',
  });

  if (!response.ok) {
    throw new Error(await readApiErrorMessage(response, '체스 리뷰 생성에 실패했습니다.'));
  }

  return normalizeReviewId(await response.json() as ChessReviewDetail | ChessReviewSummary);
};

export const deleteChessReview = async (id: number): Promise<void> => {
  const response = await fetch(`/chess/reviews/${id}`, {
    method: 'DELETE',
    credentials: 'include',
  });

  if (!response.ok) {
    throw new Error(await readApiErrorMessage(response, '체스 리뷰 삭제에 실패했습니다.'));
  }
};
