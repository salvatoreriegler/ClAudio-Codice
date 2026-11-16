package com.vladiko.voicebridge;

import android.os.Looper;
import okhttp3.*;
import okhttp3.mockwebserver.*;
import okio.ByteString;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.Shadows;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35)
@LooperMode(LooperMode.Mode.PAUSED)
public class RemoteSpeechRecognizerTest {
    private MockWebServer server;
    private RemoteSpeechRecognizer speech;
    private final List<String> events=new ArrayList<>();
    private final CountDownLatch audio=new CountDownLatch(1);
    private final CountDownLatch finished=new CountDownLatch(1);
    private final FakeMic mic=new FakeMic();
    private WebSocket peer;
    private volatile ByteString received;
    private final RemoteSpeechRecognizer.Listener listener=new RemoteSpeechRecognizer.Listener(){
        public void onReady(){events.add("ready");}
        public void onPartial(String t){events.add("partial:"+t);}
        public void onFinal(String t){events.add("final:"+t);}
        public void onDone(){events.add("done");}
        public void onError(String c){events.add("error:"+c);}
    };
    private static class FakeMic implements RemoteSpeechRecognizer.PcmSource {
        volatile boolean started,stopped,closed;
        boolean sent;
        final CountDownLatch stop=new CountDownLatch(1);
        public void start(){started=true;}
        public int read(byte[] b){
            if(!sent){sent=true;Arrays.fill(b,(byte)7);return b.length;}
            try{stop.await(5,TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}
            return -1;
        }
        public void stop(){stopped=true;stop.countDown();}
        public void close(){closed=true;}
    }
    private void open(boolean muted)throws Exception {
        server=new MockWebServer();
        server.enqueue(new MockResponse().withWebSocketUpgrade(new WebSocketListener(){
            @Override public void onOpen(WebSocket ws,Response r){peer=ws;}
            @Override public void onClosing(WebSocket ws,int code,String reason){ws.close(code,reason);}
            @Override public void onMessage(WebSocket ws,ByteString bytes){received=bytes;audio.countDown();}
            @Override public void onMessage(WebSocket ws,String text){
                if(text.contains("finish")) {
                    finished.countDown();ws.send("{\"type\":\"final\",\"text\":\"Controlla il contratto\"}");
                    ws.send("{\"type\":\"done\"}");ws.close(1000,null);
                }
            }
        }));
        server.start();
        speech=new RemoteSpeechRecognizer(new OkHttpClient(),server.url("/").toString(),"test-only","SECURDOC 1",()->muted,listener,mic);
        speech.start();
        await(()->peer!=null);
    }
    private static void await(BooleanSupplier condition)throws Exception {
        long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(6);
        while(!condition.getAsBoolean() && System.nanoTime()<end){Shadows.shadowOf(Looper.getMainLooper()).idle();Thread.sleep(10);}
        Shadows.shadowOf(Looper.getMainLooper()).idle();assertTrue("condition timed out",condition.getAsBoolean());
    }
    @After public void cleanup()throws Exception {if(speech!=null)speech.cancel();if(server!=null)server.shutdown();}
    @Test public void waitsForAuthenticationStreamsPcmAndWaitsForFinalOnFinish()throws Exception {
        open(false);assertFalse(mic.started);assertTrue(events.isEmpty());
        RecordedRequest request=server.takeRequest(3,TimeUnit.SECONDS);
        assertEquals("stt",request.getRequestUrl().queryParameter("role"));
        assertEquals("SECURDOC 1",request.getRequestUrl().queryParameter("name"));
        assertNull(request.getHeader("Authorization"));
        peer.send("{\"type\":\"ready\"}");await(()->audio.getCount()==0 && events.contains("ready"));
        assertEquals(3200,received.size());assertEquals(7,received.getByte(0));
        peer.send("{\"type\":\"partial\",\"text\":\"Controlla il contatto\"}");
        await(()->events.contains("partial:Controlla il contatto"));
        speech.finish();await(()->events.contains("done"));
        assertEquals(0,finished.getCount());assertTrue(mic.stopped);assertTrue(mic.closed);
        assertEquals(Arrays.asList("ready","partial:Controlla il contatto","final:Controlla il contratto","done"),events);
    }
    @Test public void suppressesOwnSpokenCuesAndLateCallbacksAfterCancel()throws Exception {
        open(true);peer.send("{\"type\":\"ready\"}");await(()->audio.getCount()==0);
        assertArrayEquals(new byte[3200],received.toByteArray());
        speech.cancel();int count=events.size();
        peer.send("{\"type\":\"partial\",\"text\":\"late\"}");
        await(()->mic.closed);assertEquals(count,events.size());assertTrue(mic.stopped);
    }
    @Test public void rejectedLoginNeverOpensTheMicrophone()throws Exception {
        open(false);peer.send("{\"type\":\"error\",\"code\":\"claude_login_required\"}");
        await(()->events.contains("error:claude_login_required"));
        assertFalse(mic.started);assertTrue(mic.closed);
        assertEquals(1,events.size());
    }
}
