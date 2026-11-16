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

if __name__=='__main__':unittest.main()
