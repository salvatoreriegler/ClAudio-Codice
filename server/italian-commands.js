'use strict';

// Commands must occupy the whole utterance. A sentence about sending or changing
// code must remain a message, even when it contains a command word.
const normalize = text => text.normalize('NFD').replace(/\p{M}/gu, '')
  .toLowerCase().replace(/[^a-z0-9 ]/g, ' ').replace(/\s+/g, ' ').trim();
const SEND = new Set(['invia', 'invia ora', 'invia pure', 'invia per favore']);
const CANCEL = new Set(['annulla', 'annulla messaggio']);
const numbers = {
  uno: 1, una: 1, primo: 1, prima: 1, due: 2, secondo: 2, seconda: 2,
  tre: 3, terzo: 3, terza: 3, quattro: 4, quarto: 4, quarta: 4,
  cinque: 5, quinto: 5, quinta: 5, sei: 6, sesto: 6, sesta: 6,
  sette: 7, settimo: 7, settima: 7, otto: 8, ottavo: 8, ottava: 8,
  nove: 9, nono: 9, nona: 9,
};

function parseRadioCommand(text) {
  if (/[.!?]/.test(text.trim().replace(/[.!?]+$/, ''))) return null;
  const s = normalize(text);
  if (['elenca le sessioni', 'quali sessioni', 'elenca i canali', 'quali canali'].includes(s)) return {cmd: 'list'};
  if (['torna automatico', 'torna in automatico', 'rispondi all ultima sessione'].includes(s)) return {cmd: 'unpin'};
  if (['fine comunicazione', 'spegni il microfono'].includes(s)) return {cmd: 'sleep'};
  let m = s.match(/^(?:passa|vai) (?:a|al|alla) (.+)$/);
  if (!m) return null;
  const target = m[1];
  const numeric = target.replace(/^(?:sessione|canale) /, '').replace(/ (?:sessione|canale)$/, '');
  const n = numbers[numeric] || (/^[1-9]$/.test(numeric) ? Number(numeric) : 0);
  if (n) return {cmd: 'switch', n};
  if (/^\d+$/.test(numeric)) return null;
  // Bare names use "passa a NAME"; "passa al branch main" remains prose.
  m = target.match(/^(?:sessione|canale) (.+)$/);
  const name = m ? m[1] : /^(?:passa|vai) a /.test(s) ? target : '';
  if (!/^[a-z0-9]+(?: [a-z0-9]+){0,11}$/.test(name) || /\b(?:e|poi|quindi)\b/.test(name)) return null;
  return {cmd: 'switch', n: 0, name};
}

// Whisper can transcribe several sentences in one recording. Strip only a final,
// separate command sentence; "non invia", "domani invia" etc. remain untouched.
function finalSentence(text) {
  return /(?:^|[.!?])\s*([^.!?]+)[.!?\s]*$/.exec(text);
}
function stripSend(text) {
  const m = finalSentence(text);
  if (!m || !SEND.has(normalize(m[1]))) return {text, hit: false};
  const end = m.index + (/[.!?]/.test(text[m.index]) ? 1 : 0);
  return {text: text.slice(0, end).trim(), hit: true, word: m[1].trim()};
}

function isCancel(text) {
  const m = finalSentence(text);
  return !!m && CANCEL.has(normalize(m[1]));
}
function appendTranscript(previous, segment) {
  const norm = normalize(segment);
  // A standalone command segment remains a sentence even if Whisper omitted the
  // punctuation at the end of the preceding segment.
  const boundary = previous && !/[.!?]$/.test(previous) && (SEND.has(norm) || CANCEL.has(norm));
  return (previous + (boundary ? '. ' : ' ') + segment).trim();
}

module.exports = {normalize, parseRadioCommand, stripSend, isCancel, appendTranscript};
