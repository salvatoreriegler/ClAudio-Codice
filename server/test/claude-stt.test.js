const {test}=require('node:test');
const assert=require('node:assert/strict');
const {EventEmitter}=require('node:events');
const fs=require('node:fs');
const os=require('node:os');
const path=require('node:path');
const {createSpeechBridge}=require('../claude-stt');

class Socket extends EventEmitter {
  readyState=1; bufferedAmount=0; sent=[];
  send(value) {this.sent.push(value);}
  close() {this.readyState=3;this.emit('close');}
  terminate() {this.close();}
  events() {return this.sent.filter(x=>typeof x==='string').map(x=>JSON.parse(x));}
}
function setup(t) {
  const dir=fs.mkdtempSync(path.join(os.tmpdir(),'claudio-speech-test-'));
  const file=path.join(dir,'credentials.json');
  fs.writeFileSync(file,JSON.stringify({claudeAiOauth:{accessToken:'synthetic-not-a-secret',expiresAt:Date.now()+60000}}));
  let upstream,options,url;
  const phone=new Socket();
  const bridge=createSpeechBridge({provider:'claude',credentialsFile:file},{createUpstream:(u,o)=>{
    options=o;url=u;return upstream=new Socket();
  }});
  t.after(()=>{phone.close();fs.rmSync(dir,{recursive:true,force:true});});
  return {phone,bridge,file,connect(name='SECURDOC 1') {bridge.connect(phone,name);return upstream;},
    get options(){return options;},get url(){return url;}};
}
test('streams Italian PCM, replaces partials and commits each endpoint exactly once',t=>{
  const s=setup(t), up=s.connect("RARE Dashboard|SECURDOC 1");
  assert.equal(s.phone.sent.length,0,'no premature ready');up.emit('open');
  assert.match(s.url,/language=it/);assert.match(s.options.headers['x-config-keyterms'],/SECURDOC 1/);
  assert.equal(s.options.headers.Authorization,'Bearer synthetic-not-a-secret');
  const pcm=Buffer.alloc(3200);s.phone.emit('message',pcm,true);assert.equal(up.sent[0],pcm);
  for(const e of [{type:'TranscriptText',data:'Passa a SECUR'},{type:'TranscriptText',data:'Passa a SECURDOC 1.'},
    {type:'TranscriptEndpoint'},{type:'TranscriptEndpoint'},{type:'TranscriptInterim',data:'Controlla il documento.'}])up.emit('message',Buffer.from(JSON.stringify(e)));
  s.phone.emit('message',Buffer.from('{"type":"finish"}'),false);assert.ok(up.sent.includes('{"type":"CloseStream"}'));
  up.close();
  assert.deepEqual(s.phone.events().filter(e=>e.type==='final').map(e=>e.text),['Passa a SECURDOC 1.','Controlla il documento.']);
  assert.equal(s.phone.events().at(-1).type,'done');
  assert.ok(!JSON.stringify(s.phone.events()).includes('synthetic-not-a-secret'));
});
test('reads current account credentials on every connection and rejects expired login',t=>{
  const s=setup(t);s.connect().emit('open');s.phone.close();
  fs.writeFileSync(s.file,JSON.stringify({claudeAiOauth:{accessToken:'other-synthetic-account',expiresAt:Date.now()+60000}}));
  const p=new Socket();t.after(()=>p.close());s.bridge.connect(p);
  assert.equal(s.options.headers.Authorization,'Bearer other-synthetic-account');p.close();
  fs.writeFileSync(s.file,JSON.stringify({claudeAiOauth:{accessToken:'expired',expiresAt:1}}));
  const expired=new Socket();s.bridge.connect(expired);
  assert.deepEqual(expired.events(),[{type:'error',code:'claude_login_required'}]);
});
test('transport errors preserve the last partial and never finalize it as successful',t=>{
  const s=setup(t),up=s.connect();up.emit('open');
  up.emit('message',Buffer.from('{"type":"TranscriptText","data":"Bozza da conservare"}'));
  up.emit('error',new Error('network'));up.emit('error',new Error('another error'));
  assert.deepEqual(s.phone.events().map(e=>e.type),['ready','partial','error']);
});
test('cancel suppresses late words and audio before ready or oversized frames are rejected',t=>{
  const s=setup(t),up=s.connect();up.emit('open');s.phone.emit('message',Buffer.from('{"type":"cancel"}'),false);
  up.emit('message',Buffer.from('{"type":"TranscriptText","data":"late"}'));
  assert.deepEqual(s.phone.events().map(e=>e.type),['ready']);
  const q=setup(t),uq=q.connect();uq.readyState=0;q.phone.emit('message',Buffer.alloc(3200),true);
  assert.equal(q.phone.events().at(-1).code,'not_ready');
  const r=setup(t),ur=r.connect();ur.emit('open');r.phone.emit('message',Buffer.alloc(65538),true);
  assert.equal(r.phone.events().at(-1).code,'audio_limit');
});
