const MOVE_REFERENCE = /(?<!\d)(\d{1,3})(\.\.\.|\.)([A-Za-z0-9O=+#x!?-]+)/g;
const UCI_REFERENCE = /\b([a-h][1-8][a-h][1-8][qrbn]?)\b/gi;

export function normalizeSan(value) {
  return String(value || '')
    .trim()
    .replaceAll('0', 'O')
    .replace(/[!?+#]+$/g, '')
    .replace(/e\.p\.$/i, '')
    .trim();
}

export function segmentAiFeedback(aiResponse) {
  const text = String(aiResponse || '').trim();
  if (!text) return [];
  const blankSegments = text.split(/\r?\n\s*\r?\n+/).map((item) => item.trim()).filter(Boolean);
  if (blankSegments.length > 1) return blankSegments;
  return text.split(/\r?\n(?=\s*(?:#{1,6}\s+|[-*]\s+|\d+[.)]\s+))/).map((item) => item.trim()).filter(Boolean);
}

export function matchAiFeedback(aiResponse, moves) {
  return segmentAiFeedback(aiResponse).map((segment, index) => matchSegment(segment, index, moves || []));
}

function matchSegment(text, segmentIndex, moves) {
  MOVE_REFERENCE.lastIndex = 0;
  let moveRef;
  while ((moveRef = MOVE_REFERENCE.exec(text)) !== null) {
    const moveNumber = Number(moveRef[1]);
    const side = moveRef[2] === '...' ? 'BLACK' : 'WHITE';
    const san = normalizeSan(moveRef[3]);
    const found = moves.find((move) => (
      Number(move.moveNumber) === moveNumber
      && String(move.side) === side
      && normalizeSan(move.san).toLowerCase() === san.toLowerCase()
    ));
    if (found) return fromMove(segmentIndex, text, found, 'HIGH');
  }

  UCI_REFERENCE.lastIndex = 0;
  let uciRef;
  while ((uciRef = UCI_REFERENCE.exec(text)) !== null) {
    const uci = uciRef[1].toLowerCase();
    const found = moves.find((move) => String(move.uci || '').toLowerCase() === uci);
    if (found) return fromMove(segmentIndex, text, found, 'MEDIUM');
  }

  return {
    segmentIndex,
    text,
    matchedPly: null,
    matchedMoveNumber: null,
    matchedSide: null,
    matchedSan: null,
    matchedUci: null,
    confidence: 'NONE',
    source: 'AUTO',
  };
}

function fromMove(segmentIndex, text, move, confidence) {
  return {
    segmentIndex,
    text,
    matchedPly: move.ply,
    matchedMoveNumber: move.moveNumber,
    matchedSide: move.side,
    matchedSan: move.san || null,
    matchedUci: move.uci || null,
    confidence,
    source: 'AUTO',
  };
}
