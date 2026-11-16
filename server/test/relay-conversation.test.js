const {test} = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const net = require('node:net');
const {spawn} = require('node:child_process');
const {once} = require('node:events');
const WebSocket = require('ws');

test('real relay publishes text before audio and rejects malformed/unauthorized replies', async () => {
  const probe = net.createServer();
  probe.listen(0, '127.0.0.1'); await once(probe, 'listening');
  const port = probe.address().port; await new Promise(resolve => probe.close(resolve));
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'claudio-relay-test-'));
  for(const file of ['relay.js','claude-stt.js']) fs.copyFileSync(path.join(__dirname, '..',file),path.join(dir,file));
  fs.writeFileSync(path.join(dir, 'config.json'), JSON.stringify({port, token:'test-only-token'}));
  const child = spawn(process.execPath, [path.join(dir, 'relay.js')], {
    env: {...process.env, NODE_PATH:path.resolve(__dirname, '../node_modules')}, stdio:'ignore', windowsHide:true
  });
  let phone;
  const base = `http://127.0.0.1:${port}`;
  const post = (route, data, token='test-only-token') => fetch(`${base}${route}?token=${token}`, {
    method:'POST', headers:{'content-type':'application/json'}, body:JSON.stringify(data)
  });
  try {
    let ready = false;
    for (let n=0;n<80;n++) {
      try { if ((await fetch(`${base}/status?token=test-only-token`)).ok) {ready=true; break;} } catch (_) {}
      await new Promise(r=>setTimeout(r,50));
    }
    assert.ok(ready, 'relay starts');
    phone = new WebSocket(`ws://127.0.0.1:${port}/?role=phone&token=test-only-token`);
    const events=[]; phone.on('message', b=>events.push(JSON.parse(b.toString()))); await once(phone,'open');
    const rejected = new WebSocket(`ws://127.0.0.1:${port}/?role=stt&token=invalid`);
    await assert.rejects(once(rejected,'open'), /401/);
    const reply={session:'session-a',msgid:'reply-1',proj:'SECURDOC 1',text:'Risposta completa: città e caffè.'};
    assert.equal((await post('/reply',reply,'invalid')).status,401);
    assert.equal((await post('/reply',{...reply,session:'../bad'})).status,400);
    assert.equal((await post('/reply',{...reply,text:''})).status,400);
    assert.equal((await post('/reply',reply)).status,200);
    assert.equal((await post('/reply',reply)).status,200);
    const hist=await (await fetch(`${base}/history?token=test-only-token&session=session-a`)).json();
    assert.equal(hist.events.length,1); assert.equal(hist.events[0].text,reply.text);
    assert.equal(hist.events[0].type,'transcript'); assert.equal(hist.events[0].audio,undefined);
    await new Promise(r=>setTimeout(r,50));
    assert.equal(events.filter(e=>e.type==='transcript').length,1);
    const other=await (await fetch(`${base}/history?token=test-only-token&session=session-b`)).json();
    assert.equal(other.events.length,0);
  } finally {
    if(phone)phone.terminate();
    const exited=once(child,'exit'); child.kill(); await exited;
    assert.ok(path.resolve(dir).startsWith(path.resolve(os.tmpdir()) + path.sep));
    fs.rmSync(dir,{recursive:true,force:true});
  }
});
