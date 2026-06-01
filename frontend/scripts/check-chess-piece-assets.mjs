import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, resolve } from 'node:path';

const __dirname = dirname(fileURLToPath(import.meta.url));
const assetsDir = resolve(__dirname, '../src/assets/chess-pieces');
const colors = ['white', 'black'];
const pieces = ['pawn', 'knight', 'bishop', 'rook', 'queen', 'king'];

function assert(condition, message) {
  if (!condition) throw new Error(message);
}

for (const color of colors) {
  for (const piece of pieces) {
    const file = resolve(assetsDir, `${color}-${piece}.svg`);
    const source = readFileSync(file, 'utf8');
    assert(source.includes('<svg'), `${file} must be an SVG file`);
    assert(source.includes('<title id="title">'), `${file} must include an accessible title`);
    assert(!/(?:href|src)=\"https?:\/\//i.test(source), `${file} must not hotlink remote assets`);
    assert(/stroke="#[0-9a-f]{6}"/i.test(source), `${file} must include an outline stroke for readability`);
  }
}

console.log('PASS chess SVG piece assets');
