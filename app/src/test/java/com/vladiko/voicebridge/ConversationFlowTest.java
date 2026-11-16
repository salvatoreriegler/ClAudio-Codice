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
        call(service,"startRemoteSpeech",new Class[]{boolean.class},false);
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

    private FakeSpeech monitor()throws Exception {
        set(service,"audioManager",ctx.getSystemService(Context.AUDIO_SERVICE));
        set(service,"claudeSpeechAvailable",true);set(service,"listening",false);set(service,"playing",true);
        set(service,"currentSid","securdoc-1");set(service,"playbackMessageId","reply-123");
        set(service,"playbackSpeechText","Il documento contiene tutte le informazioni richieste sulla firma digitale.");
        Cfg.INSTANCE.setReplyTarget("securdoc-1");
        FakeSpeech fake=new FakeSpeech();
        set(service,"speechFactory",(kotlin.jvm.functions.Function1<RemoteSpeechRecognizer.Listener,SpeechInput>)listener->{fake.listener=listener;return fake;});
        call(service,"startRemoteSpeech",new Class[]{boolean.class},true);
        return fake;
    }
    @Test public void voiceInterruptionIgnoresPlaybackAndKeepsFirstWordsOnSameStream()throws Exception {
        startServer();server.enqueue(new MockResponse().setBody("{\"ok\":true}"));
        FakeSpeech fake=monitor();
        fake.listener.onPartial("Il documento contiene tutte le informazioni richieste");
        fake.listener.onFinal("Il documento contiene tutte le informazioni richieste sulla firma digitale.");
        assertTrue(service.isPlaying());assertFalse(service.isListening());assertEquals("",service.liveTranscript());
        fake.listener.onPartial("Aspetta, controlla il contratto");
        assertFalse(service.isPlaying());assertTrue(service.isListening());assertFalse(fake.cancelled);
        assertEquals("controlla il contratto",service.liveTranscript());
        fake.listener.onFinal("Aspetta, controlla il contratto firmato");
        assertEquals("controlla il contratto firmato",service.liveTranscript());
        assertTrue(((java.util.Collection<?>)get(service,"skippedMsgIds")).contains("securdoc-1|reply-123"));
        service.sendNow();assertTrue(fake.finished);fake.listener.onDone();
        RecordedRequest request=server.takeRequest(5,TimeUnit.SECONDS);assertNotNull(request);
        JSONObject body=new JSONObject(request.getBody().readUtf8());
        assertEquals("securdoc-1",body.getString("session_id"));
        assertEquals("controlla il contratto firmato",body.getString("text"));
    }
    @Test public void ordinarySpeechCanInterruptWithoutACommandWord()throws Exception {
        FakeSpeech fake=monitor();
        fake.listener.onFinal("Vorrei invece controllare il budget");
        assertFalse(service.isPlaying());assertTrue(service.isListening());assertFalse(fake.cancelled);
        assertEquals("Vorrei invece controllare il budget",service.liveTranscript());
    }
    @Test public void playbackContinuesIfInterruptionMicrophoneFails()throws Exception {
        FakeSpeech fake=monitor();fake.listener.onError("microphone");
        assertTrue(service.isPlaying());assertFalse(service.isListening());assertTrue(fake.cancelled);
        assertTrue(service.playbackInterruptionHint().contains("non disponibile"));
        assertEquals("",service.liveTranscript());
    }
    @Test public void manualStopClosesPlaybackMicrophoneAndRejectsLateWords()throws Exception {
        FakeSpeech fake=monitor();service.stopPlayback(false,false);
        assertTrue(fake.cancelled);fake.listener.onFinal("Parole arrivate in ritardo");
        assertFalse(service.isPlaying());assertFalse(service.isListening());assertEquals("",service.liveTranscript());
    }
    @Test public void canonicalNamesSurviveOldAudioAndRenameSavedMessages()throws Exception {
        String sid="canonical-test";
        SessionBook.INSTANCE.seen(ctx,sid,"Ricostruzione contesto progetto",10);
        Feed.INSTANCE.rememberIn(ctx,sid,java.util.Collections.singletonList(new Feed.In("m-old",10,"Ricostruzione contesto progetto","Risposta storica",java.util.Collections.emptyList(),0)));
        SessionBook.INSTANCE.syncName(ctx,sid,"Claude WSL Ubuntu - SECURDOC 1 [12345678]",20);
        Feed.INSTANCE.renameSession(ctx,sid,SessionBook.INSTANCE.name(sid,""));
        SessionBook.INSTANCE.seen(ctx,sid,"Ricostruzione contesto progetto",30);
        assertTrue(SessionBook.INSTANCE.name(sid,"").contains("SECURDOC 1"));
        assertTrue(Feed.INSTANCE.inFor(ctx,sid).get(0).getProj().contains("SECURDOC 1"));
        SessionBook.INSTANCE.load(ctx);
        SessionBook.INSTANCE.seen(ctx,sid,"Altro titolo vecchio",40);
        SessionBook.INSTANCE.syncName(ctx,sid,"Ricostruzione contesto progetto",45);
        assertTrue(SessionBook.INSTANCE.name(sid,"").contains("SECURDOC 1"));
        SessionBook.INSTANCE.syncName(ctx,sid,"Claude WSL Ubuntu - SECURDOC 2 [12345678]",50);
        assertTrue(SessionBook.INSTANCE.name(sid,"").contains("SECURDOC 2"));
    }
    @Test @GraphicsMode(GraphicsMode.Mode.NATIVE) public void largePickerUsesOneLargeRowToSelectTheActualRecipient()throws Exception {
        Cfg.INSTANCE.setToken("visual-fixture");Cfg.INSTANCE.save(ctx);
        activityController=Robolectric.buildActivity(MainActivity.class).create();
        MainActivity activity=activityController.get();
        SessionBook.INSTANCE.seen(ctx,"picker-a","Claude WSL Ubuntu - SECURDOC 1 [12345678]",10);
        SessionBook.INSTANCE.seen(ctx,"picker-b","Claude - Collaudo 1 [abcdef12]",20);
        Cfg.INSTANCE.setReplyTarget("picker-b");
        call(activity,"openChannelPicker",new Class[]{});
        View picker=(View)get(activity,"channelPicker");assertEquals(View.VISIBLE,picker.getVisibility());
        TextView name=find((View)get(activity,"channelPickerList"),"SECURDOC 1");assertNotNull(name);
        saveScreenshot(activity,"channel-picker");
        View row=(View)name.getParent();
        assertTrue(row.getMinimumHeight()>=104);assertTrue(name.getTextSize()>=26);
        row.performClick();assertEquals("picker-a",Cfg.INSTANCE.getReplyTarget());
        assertEquals(View.GONE,picker.getVisibility());
        assertTrue(((TextView)get(activity,"chipBtn")).getText().toString().contains("SECURDOC 1"));
        saveScreenshot(activity,"conversation");
    }

    private static void saveScreenshot(MainActivity activity,String name)throws Exception {
        View root=(View)get(activity,"rootFrame");
        root.measure(View.MeasureSpec.makeMeasureSpec(412,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(892,View.MeasureSpec.EXACTLY));
        root.layout(0,0,412,892);
        String out=System.getenv("CLAUDIO_SCREENSHOTS");if(out==null)return;
        android.graphics.Bitmap bitmap=android.graphics.Bitmap.createBitmap(412,892,android.graphics.Bitmap.Config.ARGB_8888);
        root.draw(new android.graphics.Canvas(bitmap));
        java.io.File folder=new java.io.File(out);folder.mkdirs();
        try(java.io.FileOutputStream stream=new java.io.FileOutputStream(new java.io.File(folder,name+".png"))){bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,stream);}
    }
    @Test public void sessionNamesAndOldCaptionsRefreshWithoutOpeningChannels()throws Exception {
        activityController=Robolectric.buildActivity(MainActivity.class).create();
        MainActivity activity=activityController.get();startServer();
        java.util.concurrent.atomic.AtomicReference<String> title=new java.util.concurrent.atomic.AtomicReference<>("SECURDOC 1");
        server.setDispatcher(new okhttp3.mockwebserver.Dispatcher(){
            @Override public MockResponse dispatch(RecordedRequest request){
                if(request.getPath().startsWith("/sessions"))return new MockResponse().setBody("{\"sessions\":[{\"sid\":\"rename-ui\",\"proj\":\"Claude WSL Ubuntu - "+title.get()+" [12345678]\",\"lastTs\":50,\"count\":1,\"alive\":true}]}");
                return new MockResponse().setBody("{\"events\":[]}");
            }
        });
        Cfg.INSTANCE.setReplyTarget("rename-ui");
        SessionBook.INSTANCE.seen(ctx,"rename-ui","Ricostruzione contesto progetto",10);
        Feed.INSTANCE.rememberIn(ctx,"rename-ui",java.util.Collections.singletonList(new Feed.In("historic-ui",10,"Ricostruzione contesto progetto","Risposta già salvata",java.util.Collections.emptyList(),0)));
        ((Runnable)get(activity,"ticker")).run();
        awaitUi(()->((TextView)uncheckedGet(activity,"chipBtn")).getText().toString().contains("SECURDOC 1"));
        awaitUi(()->find((View)uncheckedGet(activity,"feedBox"),"SECURDOC 1")!=null);
        assertNull(find((View)get(activity,"feedBox"),"Ricostruzione contesto progetto"));
        assertEquals(View.GONE,((View)get(activity,"channelPicker")).getVisibility());
        assertEquals(View.GONE,((View)get(activity,"sheetOverlay")).getVisibility());
        title.set("SECURDOC 2");set(activity,"lastSessionsFetch",0L);
        ((Runnable)get(activity,"ticker")).run();
        awaitUi(()->find((View)uncheckedGet(activity,"feedBox"),"SECURDOC 2")!=null);
        assertTrue(Feed.INSTANCE.inFor(ctx,"rename-ui").get(0).getProj().contains("SECURDOC 2"));
        assertNull(find((View)get(activity,"feedBox"),"SECURDOC 1"));
    }
    private static Object uncheckedGet(Object o,String name){try{return get(o,name);}catch(Exception e){throw new RuntimeException(e);}}
    private static void awaitUi(java.util.function.BooleanSupplier condition)throws Exception {
        long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(8);
        while(!condition.getAsBoolean()&&System.nanoTime()<end){Shadows.shadowOf(Looper.getMainLooper()).idle();Thread.sleep(10);}
        Shadows.shadowOf(Looper.getMainLooper()).idle();assertTrue("UI condition timed out",condition.getAsBoolean());
    }

    @Test public void localAliasPersistsAndOverridesAChangingRemoteTitle()throws Exception {
        SessionBook.INSTANCE.seen(ctx,"alias-a","Claude WSL Ubuntu - SECURDOC 1 [12345678]",10);
        assertTrue(SessionBook.INSTANCE.setAlias(ctx,"alias-a","  Documenti   lavoro  "));
        SessionBook.INSTANCE.load(ctx);
        assertEquals("Documenti lavoro",SessionBook.INSTANCE.displayName("alias-a",""));
        SessionBook.INSTANCE.syncName(ctx,"alias-a","Claude WSL Ubuntu - SECURDOC 2 [12345678]",20);
        assertEquals("Documenti lavoro",SessionBook.INSTANCE.displayName("alias-a",""));
        assertTrue(SessionBook.INSTANCE.setAlias(ctx,"alias-a",""));
        assertEquals("SECURDOC 2",SessionBook.INSTANCE.displayName("alias-a",""));
    }
    @Test public void spokenLocalAliasSelectsSameSessionAsItsOriginalName()throws Exception {
        SessionBook.INSTANCE.seen(ctx,"alias-a","Claude WSL Ubuntu - SECURDOC 1 [12345678]",10);
        assertTrue(SessionBook.INSTANCE.setAlias(ctx,"alias-a","Documenti"));
        Cfg.INSTANCE.setReplyTarget("other-chat");FakeSpeech fake=remote();
        fake.listener.onFinal("Passa a Documenti");
        assertEquals("alias-a",Cfg.INSTANCE.getReplyTarget());assertEquals("Documenti",service.replyTargetName());
        assertEquals("",service.liveTranscript());
        assertNotNull(call(service,"matchSessionByName",new Class[]{String.class},"SECURDOC 1"));
    }
    @Test public void aliasesCannotCollideWithAnotherAliasOrOriginalName() {
        SessionBook.INSTANCE.seen(ctx,"alias-a","SECURDOC 1",10);SessionBook.INSTANCE.seen(ctx,"alias-b","Collaudo 1",20);
        assertTrue(SessionBook.INSTANCE.setAlias(ctx,"alias-a","Documenti"));
        assertFalse(SessionBook.INSTANCE.setAlias(ctx,"alias-b","documenti"));
        assertFalse(SessionBook.INSTANCE.setAlias(ctx,"alias-b","securdoc uno"));
        assertEquals("",SessionBook.INSTANCE.alias("alias-b"));
    }
    @Test public void mutingMicrophoneAlsoStopsVoiceInterruption()throws Exception {
        FakeSpeech fake=monitor();
        android.media.AudioManager manager=(android.media.AudioManager)get(service,"audioManager");
        manager.setMode(android.media.AudioManager.MODE_IN_COMMUNICATION);set(service,"duplexRouteActive",true);
        assertTrue(service.toggleMicMute());
        assertTrue(fake.cancelled);assertTrue(service.isPlaying());
        fake.listener.onFinal("Controlla il documento");assertEquals("",service.liveTranscript());
        assertFalse(service.isListening());
        set(service,"playing",false);call(service,"updateNotification",new Class[]{});
        assertEquals(android.media.AudioManager.MODE_NORMAL,manager.getMode());
    }

    @Test public void aliasEditorSavesLocallyAndUpdatesTheVisibleCaption()throws Exception {
        activityController=Robolectric.buildActivity(MainActivity.class).create();
        MainActivity activity=activityController.get();
        SessionBook.INSTANCE.seen(ctx,"editor-alias","Claude WSL Ubuntu - SECURDOC 1 [12345678]",10);
        Cfg.INSTANCE.setReplyTarget("editor-alias");
        call(activity,"editSessionAlias",new Class[]{String.class},"editor-alias");
        androidx.appcompat.app.AlertDialog dialog=(androidx.appcompat.app.AlertDialog)org.robolectric.shadows.ShadowDialog.getLatestDialog();
        assertNotNull(dialog);android.widget.EditText input=findEdit(dialog.getWindow().getDecorView());assertNotNull(input);
        input.setText("Documenti");dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick();
        assertEquals("Documenti",SessionBook.INSTANCE.alias("editor-alias"));
        assertTrue(((TextView)get(activity,"chipBtn")).getText().toString().contains("Documenti"));
        assertFalse(dialog.isShowing());
        call(activity,"editSessionAlias",new Class[]{String.class},"editor-alias");
        dialog=(androidx.appcompat.app.AlertDialog)org.robolectric.shadows.ShadowDialog.getLatestDialog();
        dialog.getButton(android.content.DialogInterface.BUTTON_NEUTRAL).performClick();
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertEquals("",SessionBook.INSTANCE.alias("editor-alias"));
        assertTrue(((TextView)get(activity,"chipBtn")).getText().toString().contains("SECURDOC 1"));
    }
    private static android.widget.EditText findEdit(View v) {
        if(v instanceof android.widget.EditText)return (android.widget.EditText)v;
        if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){
            android.widget.EditText found=findEdit(((ViewGroup)v).getChildAt(i));if(found!=null)return found;
        }
        return null;
    }
}
