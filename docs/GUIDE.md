# Guida a ClAudio Codice

## Primo messaggio

1. Configura il relay sul telefono e avvia `/voice-bridge` nella sessione Claude Code.
2. Chiedi alla sessione una risposta in italiano: il canale apparirà nell'app.
3. Apri **canali** e premi **rispondi** sul canale desiderato.
4. Detta il messaggio. Tocca il cerchio per inviarlo, oppure pronuncia **invia** in modalità a mani libere.

Con Android, «invia» deve essere riconosciuto come frase separata: fai una pausa fra il testo e il comando. **Invia dopo una pausa** abilita invece l'invio automatico.

## Conversazioni e destinatario

- **Attivo** indica quali canali ascolti. Puoi attivarne più di uno; nessuna selezione significa ascoltarli tutti.
- Toccare una riga apre la chat da leggere; non cambia il destinatario della voce.
- **Rispondi** sceglie il destinatario del prossimo messaggio.
- Normalmente rispondi all'ultima sessione ascoltata. Scelta manuale e dettatura già iniziata hanno precedenza.
- Fra risposte di due canali hai 12 secondi per rispondere al primo. Se inizi a parlare, la coda aspetta.

Per cambiare a voce, termina prima la bozza e pronuncia «elenca le sessioni», poi «passa alla seconda sessione». La numerazione segue l'ultimo elenco letto per tre minuti; senza elenco recente usa le quattro sessioni più recenti.

Puoi dire «passa alla sessione contabilità»: il confronto dei nomi tollera differenze, quindi ascolta la conferma del destinatario. **Torna in automatico** azzera destinatario esplicito e filtro di ascolto.

Con Android, una richiesta di navigazione durante una bozza rimane testo. Con Whisper, la navigazione deve occupare l'intera dettatura. Invia o annulla prima di cambiare canale.

## Pulsante centrale e annullamento

- Da fermo: attiva il microfono.
- Durante la dettatura: invia il messaggio.
- Durante una risposta: mette in pausa o riprende.
- Pressione prolungata: accende o spegne il servizio.
- **Interrompi e parla** interrompe la lettura per dettare.

**Annulla** cancella la bozza. Pronuncialo come frase autonoma; Whisper accetta anche una frase finale separata «Annulla» dopo il testo. **Fine comunicazione**, pronunciato da solo, cancella la bozza e mette il microfono in attesa.

## Impostazioni

**Segui automaticamente gli auricolari** segue il loro collegamento: senza auricolari l'audio può uscire dall'altoparlante. **Solo auricolari**, con modalità automatica disattivata, lascia le risposte in coda quando gli auricolari mancano.

**Modalità a mani libere** abilita il microfono continuo e l'invio vocale con Android. Attiva **riconoscimento Whisper** solo dopo aver configurato il servizio sul server.

Se un canale mostra **non riceve**, riattiva `/voice-bridge` in quella sessione. La sezione **diagnostica** mostra collegamento ed eventi; parte dei messaggi tecnici è ancora in inglese.
