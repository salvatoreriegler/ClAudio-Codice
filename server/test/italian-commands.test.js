const {test} = require('node:test');
const assert = require('node:assert/strict');
const {readFileSync} = require('node:fs');
const path = require('node:path');
const it = require('../italian-commands');
const cases = JSON.parse(readFileSync(path.join(__dirname, '../../app/src/test/resources/italian-commands.json'), 'utf8'));
for (const c of cases) test(`navigation: ${c.text}`, () => {
  const result = it.parseRadioCommand(c.text);
  if (!c.cmd) return assert.equal(result, null);
  assert.equal(result.cmd, c.cmd);
  assert.equal(result.n || 0, c.n || 0);
  assert.equal(result.name || '', c.name || '');
});
test('send only a separate final command sentence', () => {
  assert.equal(it.stripSend('Correggi il bug. Invia per favore.').text, 'Correggi il bug.');
  assert.equal(it.stripSend('Invia!').hit, true);
  for (const s of ['non invia', 'domani invia', 'il programma invia', 'non invia ora', 'invia i dati al server'])
    assert.equal(it.stripSend(s).hit, false, s);
});
test('cancel clears accumulated text only with a separate command', () => {
  assert.equal(it.isCancel('Correggi il bug. Annulla.'), true);
  for (const s of ['non annulla', 'questo comando annulla', 'annulla il deploy']) assert.equal(it.isCancel(s), false, s);
});
test('standalone command segments survive missing Whisper punctuation', () => {
  assert.equal(it.stripSend(it.appendTranscript('Correggi il bug', 'Invia')).text, 'Correggi il bug.');
  assert.equal(it.isCancel(it.appendTranscript('Correggi il bug', 'Annulla')), true);
  assert.equal(it.appendTranscript('Il programma', 'invia i dati'), 'Il programma invia i dati');
});
