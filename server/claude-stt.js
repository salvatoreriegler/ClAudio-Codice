// Bridge authenticated phone PCM to Claude Code's speech service. OAuth stays on the PC.
// This is an unofficial integration; protocol verified against the installed Claude Code client.
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const WebSocket = require('ws');
const ENDPOINT = 'wss://api.anthropic.com/api/ws/speech_to_text/voice_stream';

function createSpeechBridge(config = {}, deps = {}) {
  let active = 0;
  const enabled = config.provider === 'claude';
  const createUpstream = deps.createUpstream || ((url, options) => new WebSocket(url, options));
  function connect(phone, name = '') {
    phone.on('error', () => {});
    let upstream, keepalive, deadline, finalizer, closed = false, interim = '', finishing = false;
    const emit = event => { if (phone.readyState === WebSocket.OPEN) phone.send(JSON.stringify(event)); };
    const cleanup = () => {
      if (closed) return; closed = true;
      clearInterval(keepalive); clearTimeout(deadline); clearTimeout(finalizer);
      if (upstream) upstream.terminate();
      active--;
    };
    const fail = code => {
      if (closed) return;
      emit({type:'error', code}); cleanup();
      if (phone.readyState === WebSocket.OPEN) phone.close(1011, code);
    };
    if (!enabled || active >= 3) {
      emit({type:'error', code:enabled ? 'busy' : 'disabled'}); phone.close(1013); return;
    }
    active++;
    phone.on('close',cleanup);phone.on('error',cleanup);
    let token;
    try {
      const file = config.credentialsFile || path.join(process.env.CLAUDE_CONFIG_DIR || path.join(os.homedir(), '.claude'), '.credentials.json');
      const auth = JSON.parse(fs.readFileSync(file, 'utf8')).claudeAiOauth;
      if (!auth?.accessToken || (auth.expiresAt && auth.expiresAt < Date.now() + 10000)) throw Error('auth');
      token = auth.accessToken;
    } catch (_) { fail('claude_login_required'); return; }
    const params = new URLSearchParams({encoding:'linear16', sample_rate:'16000', channels:'1',
      endpointing_ms:'300', utterance_end_ms:'1000', language:'it', use_conversation_engine:'true'});
    const terms = ['Claude Code', 'ClAudio Codice', 'WSL', 'JSON', 'OAuth', 'localhost', ...name.split('|').slice(0,12)]
      .map(s => String(s).replace(/[^\x20-\x7E]/g,' ').replace(/,/g,' ').trim()).filter(Boolean).join(',').slice(0,1024);
    try {
      upstream = createUpstream(ENDPOINT + '?' + params, {handshakeTimeout:10000, maxPayload:256*1024,
        headers:{Authorization:'Bearer '+token, 'x-app':'cli', 'User-Agent':'ClAudio-Codice/1.48-it.4', 'x-config-keyterms':terms}});
    } catch (_) { fail('connect_failed'); return; }
    // Phone receives ready only after upstream authentication, so no initial audio is lost.
    upstream.on('open', () => {
      if (closed) return;
      if (finishing) { upstream.send('{"type":"CloseStream"}'); return; }
      emit({type:'ready',provider:'claude',language:'it'});
      keepalive=setInterval(()=>{if(upstream.readyState===WebSocket.OPEN)upstream.send('{"type":"KeepAlive"}');},8000);
      deadline=setTimeout(finish,90000);
    });
    function complete() {
      if (closed) return;
      if(interim)emit({type:'final',text:interim});
      interim=''; emit({type:'done'}); cleanup(); phone.close(1000);
    }
    function finish() {
      if(closed || finishing)return;finishing=true;
      if(upstream.readyState===WebSocket.OPEN)upstream.send('{"type":"CloseStream"}');
      finalizer=setTimeout(complete,5000);
    }
    upstream.on('message', raw => {
      if(closed)return;
      let event;try{event=JSON.parse(raw.toString());}catch(_){return;}
      if(['TranscriptText','TranscriptInterim'].includes(event.type) && typeof event.data==='string') {
        interim=event.data.slice(0,100000);emit({type:'partial',text:interim});
      } else if(event.type==='TranscriptEndpoint') {
        if(interim)emit({type:'final',text:interim});interim='';
      } else if(['TranscriptError','error'].includes(event.type))fail('transcription_failed');
    });
    upstream.on('error', error => {
      const status = /Unexpected server response: (\d+)/.exec(error.message)?.[1];
      fail(status==='401'||status==='403' ? 'claude_login_required' : 'connect_failed');
    });
    upstream.on('close', () => { if(!closed) { if(finishing)complete();else fail('connection_lost'); } });
    let bytes=0;
    phone.on('message',(data,binary)=>{
      if(closed)return;
      if(binary) {
        bytes+=data.length;
        if(finishing)return;
        if(data.length>65536 || bytes>32000*100 || data.length%2 || upstream.bufferedAmount>512*1024) {fail('audio_limit');return;}
        if(upstream.readyState!==WebSocket.OPEN){fail('not_ready');return;}
        upstream.send(data);
      } else {
        let event;try{event=JSON.parse(data.toString());}catch(_){fail('protocol');return;}
        if(event.type==='finish')finish();else if(event.type==='cancel'){cleanup();phone.close(1000);}
        else fail('protocol');
      }
    });
  }
  return {enabled, connect};
}
module.exports={createSpeechBridge};
