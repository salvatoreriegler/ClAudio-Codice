import json
import importlib.util
import pathlib
import tempfile
import unittest
from unittest.mock import patch

spec=importlib.util.spec_from_file_location('readback',pathlib.Path(__file__).with_name('readback.py'))
r=importlib.util.module_from_spec(spec);spec.loader.exec_module(r)

class ReadbackTest(unittest.TestCase):
    def test_dedup_tracks_reply_identity_not_equal_spoken_text(self):
        with tempfile.TemporaryDirectory() as d, patch.object(r,'LASTSENT',str(pathlib.Path(d)/'sent.json')):
            self.assertFalse(r.is_duplicate('chat-a-reply-1'))
            r.remember_sent('chat-a-reply-1')
            self.assertTrue(r.is_duplicate('chat-a-reply-1'))
            self.assertFalse(r.is_duplicate('chat-b-reply-1'))
            self.assertFalse(r.is_duplicate('chat-a-reply-2'))
            r.remember_sent('chat-b-reply-1')
            self.assertTrue(r.is_duplicate('chat-a-reply-1'))
    def test_old_dedup_cache_does_not_block_the_first_new_reply(self):
        with tempfile.TemporaryDirectory() as d, patch.object(r,'LASTSENT',str(pathlib.Path(d)/'sent.json')):
            pathlib.Path(r.LASTSENT).write_text('old-md5-cache')
            self.assertFalse(r.is_duplicate('reply-1'))
            r.remember_sent('reply-1');self.assertTrue(r.is_duplicate('reply-1'))

class ChannelTitleTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = pathlib.Path(self.temp.name)
        self.voice = self.root / 'voice'
        for p in [self.voice/'native', self.voice/'bridge', self.root/'sessions']:
            p.mkdir(parents=True)
        self.patch = patch.object(r, 'DIR', str(self.voice))
        self.patch.start(); self.addCleanup(self.patch.stop)
        self.transcript = self.root/'transcript.jsonl'
        self.transcript.write_text(json.dumps({'customTitle':'Ricostruzione contesto progetto'})+'\n',encoding='utf-8')
        self.payload = {'session_id':'chat-a','cwd':'/projects/securdoc'}

    def test_live_name_wins_over_old_transcript_and_native_snapshot(self):
        (self.voice/'native/chat-a.json').write_text(json.dumps({'session_id':'chat-a','owner_pid':42,
            'title':'Claude WSL Ubuntu - Vecchio nome [chat-a]'}),encoding='utf-8')
        (self.root/'sessions/42.json').write_text(json.dumps({'pid':42,'sessionId':'chat-a','name':'SECURDOC 1'}),encoding='utf-8')
        self.assertEqual(r.channel_title(self.payload,str(self.transcript)),'Claude WSL Ubuntu - SECURDOC 1 [chat-a]')
        (self.root/'sessions/42.json').write_text(json.dumps({'pid':42,'sessionId':'chat-a','name':'SECURDOC 2'}),encoding='utf-8')
        self.assertIn('SECURDOC 2',r.channel_title(self.payload,str(self.transcript)))

    def test_reused_process_cannot_supply_another_sessions_name(self):
        (self.voice/'native/chat-a.json').write_text(json.dumps({'session_id':'chat-a','owner_pid':42,
            'title':'Claude WSL Ubuntu - SECURDOC 1 [chat-a]'}),encoding='utf-8')
        (self.root/'sessions/42.json').write_text(json.dumps({'pid':42,'sessionId':'chat-b','name':'Other chat'}),encoding='utf-8')
        self.assertIn('SECURDOC 1',r.channel_title(self.payload,str(self.transcript)))

    def test_last_title_after_line_80_wins_and_cache_is_per_session(self):
        with self.transcript.open('a',encoding='utf-8') as f:
            f.write('{}\n'*100+json.dumps({'customTitle':'SECURDOC 1'})+'\n')
        self.assertEqual(r.channel_title(self.payload,str(self.transcript)),'SECURDOC 1')
        self.transcript.write_text('',encoding='utf-8')
        self.assertEqual(r.channel_title(self.payload,str(self.transcript)),'SECURDOC 1')
        (self.voice/'bridge/title_-projects-securdoc.txt').write_text('Other old chat',encoding='utf-8')
        self.assertEqual(r.channel_title({**self.payload,'session_id':'chat-b'},str(self.transcript)),'securdoc')

if __name__=='__main__':unittest.main()
