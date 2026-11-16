package com.vladiko.voicebridge;

import android.content.Context;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import org.json.JSONObject;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.android.controller.ActivityController;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.*;
import okhttp3.tls.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
@LooperMode(LooperMode.Mode.PAUSED)
public class ConversationFlowTest {
    private BridgeService service;
    private MockWebServer server;
    private ActivityController<MainActivity> activityController;
    private Context ctx;
    @Before public void setup() {
        ctx = RuntimeEnvironment.getApplication();
        ctx.getSharedPreferences("cfg",0).edit().clear().commit();
        ctx.getSharedPreferences("bridge",0).edit().clear().commit();
        Cfg.INSTANCE.load(ctx);
        Cfg.INSTANCE.setWhisper(false);
        Cfg.INSTANCE.setVoiceCues(false);
        Cfg.INSTANCE.setAutoSend(false);
        SessionBook.INSTANCE.load(ctx);
        service=Robolectric.buildService(BridgeService.class).get();
    }
    @After public void cleanup() throws Exception {
        Cfg.INSTANCE.setToken("");
        set(service,"alive",false);
        if(activityController!=null)activityController.destroy();
        if(server!=null)server.shutdown();
    }
    private static void set(Object o,String n,Object v)throws Exception {
        Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);f.set(o,v);
    }
    private static Object get(Object o,String n)throws Exception {
        Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);return f.get(o);
    }
    private static Object call(Object o,String n,Class<?>[] types,Object...args)throws Exception {
        Method m=o.getClass().getDeclaredMethod(n,types);m.setAccessible(true);return m.invoke(o,args);
    }
    private void say(String text)throws Exception {call(service,"onFinalText",new Class[]{String.class},text);}
    private void startServer()throws Exception {
        HeldCertificate cert=new HeldCertificate.Builder().addSubjectAlternativeName("localhost").build();
        server=new MockWebServer();
        server.useHttps(new HandshakeCertificates.Builder().heldCertificate(cert).build().sslSocketFactory(),false);
        server.start();
        byte[] hash=java.security.MessageDigest.getInstance("SHA-256").digest(cert.certificate().getEncoded());
        StringBuilder pin=new StringBuilder();for(byte b:hash)pin.append(String.format("%02x",b&255));
        Cfg.INSTANCE.setPin(pin.toString());
        set(service,"http",Net.INSTANCE.client(pin.toString()));
        Cfg.INSTANCE.setUrl(server.url("/").toString().replaceAll("/$",""));
        Cfg.INSTANCE.setToken("test-only");
    }
    @Test public void noRecipientKeepsTheRecognizedMessage()throws Exception {
        say("Rispondi solo prova ricevuta");
        set(service,"listening",true);
        service.sendNow();
        assertEquals("Rispondi solo prova ricevuta",service.liveTranscript());
        assertTrue(service.getConversationStatus().contains("Non inviato"));
        assertTrue(service.isListening());
    }
    @Test public void sendIncludesFinalAndStillPartialWordsInChosenChat()throws Exception {
        startServer(); server.enqueue(new MockResponse().setBody("{\"ok\":true}"));
        Cfg.INSTANCE.setReplyTarget("securdoc-1");
        say("Controlla il documento");
        set(service,"lastPartial","e verifica la firma");
        set(service,"lastPartialTs",System.currentTimeMillis());
        set(service,"listening",true);
        set(service,"audioManager",ctx.getSystemService(Context.AUDIO_SERVICE));
        service.sendNow();
        RecordedRequest request=server.takeRequest(5,TimeUnit.SECONDS);assertNotNull(request);
        JSONObject body=new JSONObject(request.getBody().readUtf8());
        assertEquals("securdoc-1",body.getString("session_id"));
        assertEquals("Controlla il documento e verifica la firma",body.getString("text"));
    }
    @Test public void recognizedSpeechSendsAfterPauseWithoutButton()throws Exception {
        startServer();server.enqueue(new MockResponse().setBody("{\"ok\":true}"));
        Cfg.INSTANCE.setReplyTarget("securdoc-1");Cfg.INSTANCE.setAutoSend(true);
        set(service,"alive",true);set(service,"listening",true);
        set(service,"audioManager",ctx.getSystemService(Context.AUDIO_SERVICE));
        say("Rispondi solo prova ricevuta");
        // Paused Looper time and the JVM wall clock are independent in Robolectric.
        set(service,"lastActivityTs",System.currentTimeMillis()-6000);
        set(service,"lastTextTs",System.currentTimeMillis()-6000);
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(6));
        RecordedRequest request=server.takeRequest(5,TimeUnit.SECONDS);assertNotNull(request);
        assertEquals("Rispondi solo prova ricevuta",new JSONObject(request.getBody().readUtf8()).getString("text"));
    }
    @Test public void visibleChannelRowSelectsActualVoiceAndTextRecipient()throws Exception {
        activityController=Robolectric.buildActivity(MainActivity.class).create();
        MainActivity activity=activityController.get();
        SessionBook.INSTANCE.seen(ctx,"securdoc-1","Claude WSL Ubuntu - SECURDOC 1 [98f0e69c]",1);
        call(activity,"rebuildSheet",new Class[]{});
        TextView name=find((View)get(activity,"sheetList"),"SECURDOC 1");assertNotNull(name);
        View row=(View)name.getParent().getParent();
        assertTrue(row.getMinimumHeight()>=88);
        row.performClick();
        assertEquals("securdoc-1",Cfg.INSTANCE.getReplyTarget());
        assertEquals("securdoc-1",call(activity,"currentSid",new Class[]{}));
    }
    private static TextView find(View v,String text) {
        if(v instanceof TextView && ((TextView)v).getText().toString().contains(text))return (TextView)v;
        if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++) {
            TextView r=find(((ViewGroup)v).getChildAt(i),text);if(r!=null)return r;
        }
        return null;
    }
    @Test public void migrationEnablesHandsFreeOnceAndPreservesLaterChoice() {
        ctx.getSharedPreferences("cfg",0).edit().clear().putBoolean("handsFree",false).commit();
        Cfg.INSTANCE.load(ctx); assertTrue(Cfg.INSTANCE.getHandsFree()); assertTrue(Cfg.INSTANCE.getAutoSend());
        Cfg.INSTANCE.setHandsFree(false);Cfg.INSTANCE.save(ctx);Cfg.INSTANCE.load(ctx);
        assertFalse(Cfg.INSTANCE.getHandsFree());
    }
    @Test public void transcriptBeforeAudioIsVisibleWithoutDuplicatingText()throws Exception {
        activityController=Robolectric.buildActivity(MainActivity.class).create();
        MainActivity activity=activityController.get();
        String json="{\"events\":[{\"type\":\"transcript\",\"msgid\":\"m1\",\"text\":\"Risposta completa\",\"ts\":1},"+
            "{\"type\":\"voice\",\"msgid\":\"m1\",\"part\":1,\"text\":\"Risposta completa\",\"audio\":\"/audio/one.ogg\",\"ts\":2}]}";
        java.util.List<?> msgs=(java.util.List<?>)call(activity,"parseHistory",new Class[]{String.class},json);
        assertEquals(1,msgs.size());
        assertEquals(1,((java.util.List<?>)get(msgs.get(0),"texts")).size());
        assertEquals(1,((java.util.List<?>)get(msgs.get(0),"audios")).size());
    }
    @Test public void androidServiceVisibilityIncludesSpeechRecognition()throws Exception {
        Path p=Paths.get("src/main/AndroidManifest.xml");
        if(!Files.exists(p))p=Paths.get("app/src/main/AndroidManifest.xml");
        org.w3c.dom.Document doc=javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(p.toFile());
        org.w3c.dom.NodeList queries=doc.getElementsByTagName("queries");assertEquals(1,queries.getLength());
        String xml=new String(Files.readAllBytes(p), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(xml.contains("android.speech.RecognitionService"));
        assertTrue(xml.contains("android.intent.action.TTS_SERVICE"));
    }

    private static class FakeSpeech implements SpeechInput {
        RemoteSpeechRecognizer.Listener listener;
        boolean finished, cancelled, started;
        public void start(){started=true;listener.onReady();}
        public void finish(){finished=true;}
        public void cancel(){cancelled=true;}
    }
    private FakeSpeech remote()throws Exception {
        set(service,"audioManager",ctx.getSystemService(Context.AUDIO_SERVICE));
        set(service,"claudeSpeechAvailable",true);
        set(service,"listening",true);
        FakeSpeech fake=new FakeSpeech();
        set(service,"speechFactory",(kotlin.jvm.functions.Function1<RemoteSpeechRecognizer.Listener,SpeechInput>)listener->{fake.listener=listener;return fake;});
        call(service,"startRemoteSpeech",new Class[]{});
        assertTrue(fake.started);assertTrue(service.recognitionEngineName().contains("Claude"));
        return fake;
    }
    @Test public void claudeWaitsForFinalCorrectionBeforeSendingToChosenChat()throws Exception {
        startServer();server.enqueue(new MockResponse().setBody("{\"ok\":true}"));
        Cfg.INSTANCE.setReplyTarget("securdoc-1");
        FakeSpeech fake=remote();
        fake.listener.onPartial("Controlla i contatti");
        assertEquals("Controlla i contatti",service.liveTranscript());
        service.sendNow();assertTrue(fake.finished);
        assertNull(server.takeRequest(100,TimeUnit.MILLISECONDS));
        fake.listener.onPartial("Controlla i contratti");
        fake.listener.onFinal("Controlla i contratti");fake.listener.onDone();
        RecordedRequest request=server.takeRequest(5,TimeUnit.SECONDS);assertNotNull(request);
        JSONObject body=new JSONObject(request.getBody().readUtf8());
        assertEquals("securdoc-1",body.getString("session_id"));
        assertEquals("Controlla i contratti",body.getString("text"));
        fake.listener.onFinal("Evento tardivo");
        assertEquals("",service.liveTranscript());
    }
    @Test public void claudeAutoSendFinalizesWithoutATap()throws Exception {
        startServer();server.enqueue(new MockResponse().setBody("{\"ok\":true}"));
        Cfg.INSTANCE.setReplyTarget("securdoc-1");Cfg.INSTANCE.setAutoSend(true);
        FakeSpeech fake=remote();fake.listener.onPartial("Rispondi solo prova ricevuta");
        set(service,"lastActivityTs",System.currentTimeMillis()-7000);
        set(service,"lastTextTs",System.currentTimeMillis()-7000);
        set(service,"lastVoiceTs",System.currentTimeMillis()-7000);
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(6));
        assertTrue(fake.finished);assertNull(server.takeRequest(100,TimeUnit.MILLISECONDS));
        fake.listener.onFinal("Rispondi solo prova ricevuta.");fake.listener.onDone();
        RecordedRequest request=server.takeRequest(5,TimeUnit.SECONDS);assertNotNull(request);
        assertEquals("Rispondi solo prova ricevuta.",new JSONObject(request.getBody().readUtf8()).getString("text"));
    }
    @Test public void claudeFailureKeepsAllWordsAndCancelsAutomaticSend()throws Exception {
        Cfg.INSTANCE.setAutoSend(true);FakeSpeech fake=remote();
        fake.listener.onFinal("Controlla il documento");
        fake.listener.onPartial("e la firma");fake.listener.onError("connection_lost");
        assertFalse(service.isListening());assertTrue(fake.cancelled);
        assertEquals("Controlla il documento e la firma",service.liveTranscript());
        assertTrue(service.getConversationStatus().startsWith("Non inviato"));
        assertNull(get(service,"autoSendTimer"));
        fake.listener.onFinal("Questo evento vecchio va ignorato");
        assertEquals("Controlla il documento e la firma",service.liveTranscript());
    }
    @Test public void claudeVoiceCommandChangesChannelWithoutBecomingAPrompt()throws Exception {
        SessionBook.INSTANCE.seen(ctx,"securdoc-1","Claude WSL Ubuntu - SECURDOC 1 [98f0e69c]",1);
        Cfg.INSTANCE.setReplyTarget("another-chat");FakeSpeech fake=remote();
        fake.listener.onPartial("Passa a SECURDOC 1.");fake.listener.onFinal("Passa a SECURDOC 1.");
        assertEquals("securdoc-1",Cfg.INSTANCE.getReplyTarget());
        assertEquals("",service.liveTranscript());assertTrue(fake.cancelled);
    }
}
