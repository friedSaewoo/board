import { matchAiFeedback } from '../src/utils/chessFeedbackMatcher.mjs';

const moves = [
  { ply: 1, moveNumber: 1, side: 'WHITE', san: 'e4', uci: 'e2e4' },
  { ply: 2, moveNumber: 1, side: 'BLACK', san: 'e5', uci: 'e7e5' },
  { ply: 5, moveNumber: 3, side: 'WHITE', san: 'exd5', uci: 'e4d5' },
  { ply: 6, moveNumber: 3, side: 'BLACK', san: 'Bf5', uci: 'c8f5' },
  { ply: 8, moveNumber: 4, side: 'BLACK', san: 'cxd5', uci: 'c6d5' },
  { ply: 26, moveNumber: 13, side: 'BLACK', san: 'O-O', uci: 'e8g8' },
  { ply: 44, moveNumber: 22, side: 'BLACK', san: 'Qh3', uci: 'd7h3' },
];

function assert(condition, message) {
  if (!condition) throw new Error(message);
}

function single(text) {
  return matchAiFeedback(text, moves)[0];
}

assert(single('13...O-O? 이후 킹 안전이 좋아졌습니다.').matchedPly === 26, '13...O-O? should match black castling');
assert(single('22...Qh3! 전술 기회를 만들었습니다.').matchedPly === 44, '22...Qh3! should strip punctuation and match');
assert(single('3...Bf5?! 대신 3...cxd5! 도 고려할 수 있습니다.').matchedPly === 6, 'primary reference should win in multi-reference paragraph');
assert(single('초반 e2e4 선택은 공간 확보에 좋습니다.').matchedPly === 1, 'UCI fallback should match e2e4');
assert(single('이 문단은 특정 수를 언급하지 않습니다.').confidence === 'NONE', 'no move reference should be NONE');
assert(single('99...Qh3? 은 실제 경기 범위를 벗어납니다.').confidence === 'NONE', 'out of range should not throw and should be NONE');

console.log('PASS chess feedback matcher fixtures');
