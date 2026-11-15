package com.vladiko.voicebridge;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
@LooperMode(LooperMode.Mode.PAUSED)
public class ItalianCommandsTest {
    private BridgeService service;
    @Before public void setup() {
        Context ctx = RuntimeEnvironment.getApplication();
        ctx.getSharedPreferences("cfg", 0).edit().clear().commit();
        ctx.getSharedPreferences("bridge", 0).edit().clear().commit();
        Cfg.INSTANCE.load(ctx);
        Cfg.INSTANCE.setWhisper(false);
        Cfg.INSTANCE.setVoiceCues(false);
        Cfg.INSTANCE.setHandsFree(true);
        Cfg.INSTANCE.setAutoSend(false);
        SessionBook.INSTANCE.load(ctx);
        SessionBook.INSTANCE.seen(ctx, "session-a", "Contabilità", 1000L);
        SessionBook.INSTANCE.seen(ctx, "session-b", "Sito", 2000L);
        service = Robolectric.buildService(BridgeService.class).get();
    }
    private void say(String text) throws Exception {
        Method m = BridgeService.class.getDeclaredMethod("onFinalText", String.class);
        m.setAccessible(true); m.invoke(service, text);
    }
    private StringBuilder draft() throws Exception {
        Field f = BridgeService.class.getDeclaredField("draft");
        f.setAccessible(true); return (StringBuilder)f.get(service);
    }
    @Test public void matchesSharedCorpus() throws Exception {
        String data = new String(getClass().getResourceAsStream("/italian-commands.json").readAllBytes(), StandardCharsets.UTF_8);
        JSONArray cases = new JSONArray(data);
        for (int i=0; i<cases.length(); i++) {
            JSONObject c = cases.getJSONObject(i);
            ItalianCommands.Command cmd = ItalianCommands.INSTANCE.parse(c.getString("text"));
            if (c.isNull("cmd")) { assertNull(c.getString("text"), cmd); continue; }
            assertNotNull(c.getString("text"), cmd);
            assertEquals(c.getString("cmd"), cmd.getCmd());
            assertEquals(c.optInt("n", 0), cmd.getN());
            assertEquals(c.optString("name", ""), cmd.getName());
        }
    }
    @Test public void androidCommandChangesRecipientAndIsNotDictated() throws Exception {
        say("Passa alla seconda sessione");
        assertEquals("session-a", Cfg.INSTANCE.getReplyTarget());
        assertEquals("", draft().toString());
    }
    @Test public void navigationDoesNotStealAnExistingDraft() throws Exception {
        Cfg.INSTANCE.setReplyTarget("session-a");
        draft().append("Correggi il bug.");
        say("passa alla prima");
        assertEquals("session-a", Cfg.INSTANCE.getReplyTarget());
        assertTrue(draft().toString().startsWith("Correggi il bug."));
    }
    @Test public void normalProseRemainsTextAndStandaloneCancelClearsIt() throws Exception {
        say("Il programma invia i dati");
        assertEquals("Il programma invia i dati", draft().toString());
        say("Annulla");
        assertEquals("", draft().toString());
    }
    @Test public void italianLocaleAndSeparateAppIdentity() {
        assertEquals("it-IT", Lang.INSTANCE.getRecognizerTag());
        assertEquals("it", Lang.INSTANCE.getTtsLocale().getLanguage());
        assertEquals("it.claudiocodice.app", BuildConfig.APPLICATION_ID);
        Context ctx = RuntimeEnvironment.getApplication();
        assertEquals("ClAudio Codice", ctx.getApplicationInfo().loadLabel(ctx.getPackageManager()));
    }
}
