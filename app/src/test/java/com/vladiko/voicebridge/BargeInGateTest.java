package com.vladiko.voicebridge;

import org.junit.Test;
import static org.junit.Assert.*;

public class BargeInGateTest {
    private final String spoken="Il documento contiene tutte le informazioni richieste sulla firma digitale.";
    @Test public void playbackAndMinorRecognitionErrorsDoNotInterrupt() {
        BargeInGate gate=new BargeInGate();
        assertNull(gate.accept("tutte le informazioni richieste",spoken,100,true));
        assertNull(gate.accept("Il documento contiene tutte le informazione richieste sulla firma digitale",spoken,200,true));
        assertNull(gate.accept("stop","Per fermare tutto puoi dire stop",300,true));
    }
    @Test public void normalSpeechNeedsStableWordsOrAnEndpoint() {
        BargeInGate gate=new BargeInGate();
        assertNull(gate.accept("Vorrei cambiare",spoken,100,false));
        assertEquals("Vorrei cambiare progetto",gate.accept("Vorrei cambiare progetto",spoken,350,false));
        assertEquals("Controlla invece il budget",new BargeInGate().accept("Controlla invece il budget",spoken,100,true));
    }
    @Test public void explicitInterruptionPreservesTheFollowingRequest() {
        assertEquals("Aspetta, controlla il contratto",new BargeInGate().accept("Aspetta, controlla il contratto",spoken,100,false));
        assertEquals("controlla il contratto",BargeInGate.withoutInterruptCommand("Aspetta, controlla il contratto"));
        assertEquals("",BargeInGate.withoutInterruptCommand("Fermati!"));
        assertEquals("Aspetta cambia il progetto",BargeInGate.trimEcho("tutte le informazioni richieste Aspetta cambia il progetto",spoken));
    }
}
